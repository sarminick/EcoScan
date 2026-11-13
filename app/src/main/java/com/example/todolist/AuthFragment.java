package com.example.todolist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/** Firebase Authentication screen restricted to @unisimon.edu.co accounts. */
public class AuthFragment extends Fragment {
    public static final String REQUIRED_DOMAIN = "@unisimon.edu.co";

    private boolean registerMode;
    private EditText etName, etEmail, etPassword;
    private TextView tvStatus, tvTitle;
    private MaterialButton btnPrimary, btnToggle, btnReset, btnVerify, btnCheckVerified, btnSignOut;

    public static AuthFragment newInstance(boolean register) {
        AuthFragment f = new AuthFragment();
        Bundle b = new Bundle();
        b.putBoolean("register", register);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view = inflater.inflate(R.layout.fragment_auth, parent, false);

        tvTitle = view.findViewById(R.id.tvAuthTitle);
        tvStatus = view.findViewById(R.id.tvAuthStatus);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnPrimary = view.findViewById(R.id.btnAuthPrimary);
        btnToggle = view.findViewById(R.id.btnToggleAuth);
        btnReset = view.findViewById(R.id.btnResetPassword);
        btnVerify = view.findViewById(R.id.btnVerifyEmail);
        btnCheckVerified = view.findViewById(R.id.btnCheckVerified);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        registerMode = getArguments() != null && getArguments().getBoolean("register", false);

        btnPrimary.setOnClickListener(v -> authenticate());
        btnToggle.setOnClickListener(v -> {
            registerMode = !registerMode;
            render();
        });
        btnReset.setOnClickListener(v -> resetPassword());
        btnVerify.setOnClickListener(v -> sendVerification(false));
        btnCheckVerified.setOnClickListener(v -> checkEmailVerificationStatus());
        btnSignOut.setOnClickListener(v -> {
            FirebaseGateway.auth().signOut();
            render();
        });

        render();
        return view;
    }

    private boolean configured() {
        if (FirebaseGateway.isConfigured(requireContext())) return true;
        tvStatus.setText("Firebase aún no está configurado en esta compilación (falta google-services.json).");
        return false;
    }

    private void render() {
        if (!configured()) return;
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        boolean loggedIn = user != null;

        if (loggedIn) {
            etName.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
            etPassword.setVisibility(View.GONE);
            btnPrimary.setVisibility(View.GONE);
            btnToggle.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);

            boolean verified = user.isEmailVerified();
            btnVerify.setVisibility(verified ? View.GONE : View.VISIBLE);
            btnCheckVerified.setVisibility(verified ? View.GONE : View.VISIBLE);

            tvTitle.setText("Sesión activa");
            if (verified) {
                tvStatus.setTextColor(getResources().getColor(R.color.unisimon_green, null));
                tvStatus.setText("Cuenta: " + user.getEmail() + "\n✓ Correo verificado institucionalmente.");
            } else {
                tvStatus.setTextColor(0xFFD32F2F);
                tvStatus.setText("Cuenta: " + user.getEmail() + "\n⚠ Correo pendiente de verificación.\nRevisa tu bandeja de entrada institucional.");
            }
        } else {
            tvTitle.setText(registerMode ? "Crear cuenta Unisimón" : "Iniciar sesión");
            etName.setVisibility(registerMode ? View.VISIBLE : View.GONE);
            etEmail.setVisibility(View.VISIBLE);
            etPassword.setVisibility(View.VISIBLE);
            btnPrimary.setVisibility(View.VISIBLE);
            btnPrimary.setText(registerMode ? "Registrarse" : "Iniciar sesión");
            btnToggle.setVisibility(View.VISIBLE);
            btnToggle.setText(registerMode ? "¿Ya tienes cuenta? Inicia sesión" : "¿No tienes cuenta? Regístrate");
            btnReset.setVisibility(registerMode ? View.GONE : View.VISIBLE);
            btnVerify.setVisibility(View.GONE);
            btnCheckVerified.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.GONE);
            tvStatus.setText("");
        }
    }

    private void authenticate() {
        if (!configured()) return;
        String mail = etEmail.getText().toString().trim().toLowerCase();
        String pass = etPassword.getText().toString();

        if (TextUtils.isEmpty(mail)) {
            tvStatus.setTextColor(0xFFD32F2F);
            tvStatus.setText("Ingresa tu correo institucional.");
            return;
        }

        if (!mail.endsWith(REQUIRED_DOMAIN)) {
            tvStatus.setTextColor(0xFFD32F2F);
            tvStatus.setText("Acceso denegado: Solo se permiten correos institucionales terminados en " + REQUIRED_DOMAIN);
            return;
        }

        if (pass.length() < 6) {
            tvStatus.setTextColor(0xFFD32F2F);
            tvStatus.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        btnPrimary.setEnabled(false);

        if (registerMode) {
            String fullName = etName.getText().toString().trim();
            if (TextUtils.isEmpty(fullName)) {
                btnPrimary.setEnabled(true);
                tvStatus.setTextColor(0xFFD32F2F);
                tvStatus.setText("Ingresa tu nombre completo.");
                return;
            }

            FirebaseGateway.auth().createUserWithEmailAndPassword(mail, pass)
                    .addOnCompleteListener(requireActivity(), t -> {
                        btnPrimary.setEnabled(true);
                        if (t.isSuccessful()) {
                            FirebaseUser newUser = FirebaseGateway.auth().getCurrentUser();
                            FirebaseGateway.createProfile(newUser, fullName);
                            sendVerification(true);
                            Toast.makeText(requireContext(), "Cuenta creada. Por favor verifica tu correo.", Toast.LENGTH_LONG).show();
                            ((MainActivity) requireActivity()).enterAuthenticatedExperience();
                        } else {
                            String err = getReadableRegisterErrorMessage(t.getException());
                            tvStatus.setTextColor(0xFFD32F2F);
                            tvStatus.setText(err);
                        }
                    });
        } else {
            FirebaseGateway.auth().signInWithEmailAndPassword(mail, pass)
                    .addOnCompleteListener(requireActivity(), t -> {
                        btnPrimary.setEnabled(true);
                        if (t.isSuccessful()) {
                            FirebaseUser u = FirebaseGateway.auth().getCurrentUser();
                            if (u != null && u.getEmail() != null && !u.getEmail().toLowerCase().endsWith(REQUIRED_DOMAIN)) {
                                FirebaseGateway.auth().signOut();
                                tvStatus.setTextColor(0xFFD32F2F);
                                tvStatus.setText("Solo estudiantes y comunidad con correo " + REQUIRED_DOMAIN + " pueden ingresar.");
                                return;
                            }

                            // If account exists but email is not verified:
                            if (u != null && !u.isEmailVerified()) {
                                tvStatus.setTextColor(0xFFD32F2F);
                                tvStatus.setText("Debes verificar tu correo institucional antes de continuar.\nRevisa tu bandeja de entrada o haz clic en reenviar.");
                                btnVerify.setVisibility(View.VISIBLE);
                                btnCheckVerified.setVisibility(View.VISIBLE);

                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Verificación pendiente")
                                        .setMessage("Tu cuenta existe pero tu correo institucional (" + u.getEmail() + ") aún no está verificado.\n\nDebes verificar tu correo antes de poder continuar.")
                                        .setPositiveButton("Reenviar verificación", (dialog, which) -> sendVerification(false))
                                        .setNeutralButton("Ya lo verifiqué", (dialog, which) -> checkEmailVerificationStatus())
                                        .setNegativeButton("Cerrar", null)
                                        .show();
                                return;
                            }

                            ((MainActivity) requireActivity()).enterAuthenticatedExperience();
                        } else {
                            String userMessage = getReadableLoginErrorMessage(t.getException());
                            tvStatus.setTextColor(0xFFD32F2F);
                            tvStatus.setText(userMessage);
                            Toast.makeText(requireContext(), userMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Differentiates and maps Firebase Authentication errors into clear user-friendly messages.
     */
    private String getReadableLoginErrorMessage(@Nullable Exception exception) {
        if (exception == null) {
            return "Ocurrió un error inesperado al iniciar sesión.";
        }

        // Direct instance check for non-existent user
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "No encontramos una cuenta registrada con este correo. Por favor, crea una cuenta para continuar.";
        }

        String errorCode = "";
        if (exception instanceof FirebaseAuthException) {
            errorCode = ((FirebaseAuthException) exception).getErrorCode();
        }

        String codeUpper = errorCode != null ? errorCode.toUpperCase() : "";
        String codeLower = errorCode != null ? errorCode.toLowerCase() : "";
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        // 1. user-not-found: non-existent account
        if (codeUpper.contains("USER_NOT_FOUND") || codeLower.contains("user-not-found")
                || message.contains("user-not-found") || message.contains("no user record")) {
            return "No encontramos una cuenta registrada con este correo. Por favor, crea una cuenta para continuar.";
        }

        // 2. wrong-password / invalid-credential
        if (codeUpper.contains("WRONG_PASSWORD") || codeLower.contains("wrong-password")
                || codeUpper.contains("INVALID_CREDENTIAL") || codeLower.contains("invalid-credential")
                || message.contains("wrong-password") || message.contains("invalid-credential")
                || message.contains("invalid password") || message.contains("password is invalid")) {
            return "Contraseña incorrecta o credenciales inválidas. Verifica tus datos e intenta nuevamente.";
        }

        // 3. invalid-email
        if (codeUpper.contains("INVALID_EMAIL") || codeLower.contains("invalid-email")
                || message.contains("invalid-email") || message.contains("badly formatted")) {
            return "El formato del correo electrónico no es válido.";
        }

        // 4. too-many-requests
        if (codeUpper.contains("TOO_MANY_REQUESTS") || codeLower.contains("too-many-requests")
                || message.contains("too-many-requests") || message.contains("blocked all requests")
                || message.contains("unusual activity")) {
            return "Demasiados intentos fallidos. Por favor, espera unos minutos antes de intentar de nuevo.";
        }

        // 5. network-request-failed
        if (codeUpper.contains("NETWORK_REQUEST_FAILED") || codeLower.contains("network-request-failed")
                || exception instanceof FirebaseNetworkException
                || message.contains("network-request-failed") || message.contains("network error")) {
            return "Error de conexión. Revisa tu acceso a internet e intenta nuevamente.";
        }

        // 6. user-disabled
        if (codeUpper.contains("USER_DISABLED") || codeLower.contains("user-disabled")
                || message.contains("user-disabled")) {
            return "Esta cuenta ha sido inhabilitada. Contacta al administrador institucional.";
        }

        return "No fue posible iniciar sesión. Verifica tus credenciales e intenta nuevamente.";
    }

    private String getReadableRegisterErrorMessage(@Nullable Exception exception) {
        if (exception == null) {
            return "Ocurrió un error al crear la cuenta.";
        }

        String errorCode = "";
        if (exception instanceof FirebaseAuthException) {
            errorCode = ((FirebaseAuthException) exception).getErrorCode();
        }

        String codeUpper = errorCode != null ? errorCode.toUpperCase() : "";
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        if (codeUpper.contains("EMAIL_ALREADY_IN_USE") || message.contains("email-already-in-use")) {
            return "Ya existe una cuenta registrada con este correo. Por favor, inicia sesión.";
        }

        if (codeUpper.contains("WEAK_PASSWORD") || message.contains("weak-password")) {
            return "La contraseña es muy débil. Utiliza al menos 6 caracteres combinando letras y números.";
        }

        if (codeUpper.contains("INVALID_EMAIL") || message.contains("invalid-email")) {
            return "El formato del correo electrónico no es válido.";
        }

        if (codeUpper.contains("NETWORK_REQUEST_FAILED") || exception instanceof FirebaseNetworkException || message.contains("network")) {
            return "Error de conexión. Revisa tu acceso a internet e intenta nuevamente.";
        }

        return "No fue posible crear la cuenta. Por favor verifica los datos e intenta nuevamente.";
    }

    private void resetPassword() {
        if (!configured()) return;
        String mail = etEmail.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(mail)) {
            tvStatus.setTextColor(0xFFD32F2F);
            tvStatus.setText("Escribe tu correo institucional primero.");
            return;
        }
        if (!mail.endsWith(REQUIRED_DOMAIN)) {
            tvStatus.setTextColor(0xFFD32F2F);
            tvStatus.setText("El correo debe terminar en " + REQUIRED_DOMAIN);
            return;
        }
        FirebaseGateway.auth().sendPasswordResetEmail(mail)
                .addOnCompleteListener(requireActivity(), t -> {
                    if (t.isSuccessful()) {
                        Toast.makeText(requireContext(), "Enviamos el enlace de restablecimiento a tu correo.", Toast.LENGTH_LONG).show();
                        tvStatus.setTextColor(getResources().getColor(R.color.unisimon_green, null));
                        tvStatus.setText("Revisa tu correo para restablecer la contraseña.");
                    } else {
                        tvStatus.setTextColor(0xFFD32F2F);
                        tvStatus.setText("No fue posible enviar el enlace: " + getReadableLoginErrorMessage(t.getException()));
                    }
                });
    }

    private void sendVerification(boolean initial) {
        if (!configured()) return;
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null) return;
        user.sendEmailVerification().addOnCompleteListener(requireActivity(), t -> {
            if (t.isSuccessful()) {
                String msg = initial
                        ? "Te enviamos un correo de verificación a " + user.getEmail() + ". Debes verificarlo antes de reportar."
                        : "Correo de verificación reenviado a " + user.getEmail();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                render();
            } else {
                Toast.makeText(requireContext(), "Error al enviar verificación: " + getReadableLoginErrorMessage(t.getException()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkEmailVerificationStatus() {
        FirebaseUser user = FirebaseGateway.auth().getCurrentUser();
        if (user == null) return;
        user.reload().addOnCompleteListener(requireActivity(), t -> {
            if (t.isSuccessful()) {
                if (user.isEmailVerified()) {
                    Toast.makeText(requireContext(), "¡Correo verificado con éxito!", Toast.LENGTH_SHORT).show();
                    ((MainActivity) requireActivity()).enterAuthenticatedExperience();
                } else {
                    Toast.makeText(requireContext(), "El correo aún no ha sido verificado. Por favor abre el enlace que enviamos a " + user.getEmail(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "No se pudo actualizar el estado: " + getReadableLoginErrorMessage(t.getException()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
