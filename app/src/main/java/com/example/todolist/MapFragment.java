package com.example.todolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Displays approved campus locations and pending community points with visual differentiation. */
public class MapFragment extends Fragment implements PointDetailBottomSheetDialogFragment.PointActionListener {
    private final List<RecyclingPoint> points = new ArrayList<>();
    private final List<String> targetCategories = new ArrayList<>();
    private LinearLayout container;
    private TextView empty, count;
    private String activeFilter;

    private boolean isMapFullscreen = false;
    private View layoutMapHeader, layoutMapBottomContent, mapTemplateContainer;
    private android.widget.ImageButton btnMapFullscreen;
    private androidx.activity.OnBackPressedCallback backCallback;

    public static MapFragment newInstance(@Nullable ArrayList<String> targetCategories) {
        MapFragment fragment = new MapFragment();
        if (targetCategories != null && !targetCategories.isEmpty()) {
            Bundle args = new Bundle();
            args.putStringArrayList("target_categories", targetCategories);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View view = inflater.inflate(R.layout.fragment_map, parent, false);
        container = view.findViewById(R.id.pointsContainer);
        empty = view.findViewById(R.id.tvEmptyPoints);
        count = view.findViewById(R.id.tvPointCount);

        layoutMapHeader = view.findViewById(R.id.layoutMapHeader);
        layoutMapBottomContent = view.findViewById(R.id.layoutMapBottomContent);
        mapTemplateContainer = view.findViewById(R.id.mapTemplateContainer);
        btnMapFullscreen = view.findViewById(R.id.btnMapFullscreen);

        if (getArguments() != null && getArguments().containsKey("target_categories")) {
            ArrayList<String> cats = getArguments().getStringArrayList("target_categories");
            if (cats != null) targetCategories.addAll(cats);
        }

        if (btnMapFullscreen != null) {
            btnMapFullscreen.setOnClickListener(v -> toggleMapFullscreen());
        }

        backCallback = new androidx.activity.OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isMapFullscreen) {
                    toggleMapFullscreen();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        view.findViewById(R.id.btnMapReportPoint).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openReportBottomSheet(null);
            }
        });

        view.findViewById(R.id.chipFilterAll).setOnClickListener(v -> selectFilter(null));
        view.findViewById(R.id.chipFilterPending).setOnClickListener(v -> selectFilter("pending"));
        view.findViewById(R.id.chipFilterWhite).setOnClickListener(v -> selectFilter("sede 2"));
        view.findViewById(R.id.chipFilterGreen).setOnClickListener(v -> selectFilter("sede 3"));
        view.findViewById(R.id.chipFilterBlack).setOnClickListener(v -> selectFilter("plást"));

        reloadAllPoints();
        return view;
    }

    private void reloadAllPoints() {
        points.clear();
        points.addAll(loadOfficialPoints());
        selectFilter(activeFilter);
        loadFirestorePoints();
    }

    private void selectFilter(@Nullable String filter) {
        activeFilter = filter;
        showPoints(filter);
    }

    private void loadFirestorePoints() {
        if (!FirebaseGateway.isConfigured(requireContext())) return;

        // Load approved community points
        FirebaseGateway.approvedPoints((documents, error) -> {
            if (error == null && documents != null && isAdded()) {
                for (DocumentSnapshot doc : documents) {
                    // Check duplicate
                    boolean exists = false;
                    for (RecyclingPoint p : points) {
                        if (p.id.equals(doc.getId())) { exists = true; break; }
                    }
                    if (!exists) {
                        String name = "Punto comunitario — " + safe(doc.getString("site"));
                        points.add(new RecyclingPoint(
                                doc.getId(),
                                name,
                                safe(doc.getString("site")),
                                safe(doc.getString("block")),
                                safe(doc.getString("location")),
                                "community",
                                safe(doc.getString("materialsAccepted")),
                                "approved",
                                doc.getString("photo"),
                                doc.getString("coordinates"),
                                doc.getString("userId"),
                                new ArrayList<>()
                        ));
                    }
                }
                showPoints(activeFilter);
            }

            // Load pending collaborative reports
            FirebaseGateway.pendingReports((pendingDocs, pError) -> {
                if (pError == null && pendingDocs != null && isAdded()) {
                    for (DocumentSnapshot pDoc : pendingDocs) {
                        String name = "Reporte: " + safe(pDoc.getString("site"));
                        List<?> rawConf = (List<?>) pDoc.get("confirmations");
                        ArrayList<String> confs = new ArrayList<>();
                        if (rawConf != null) {
                            for (Object item : rawConf) if (item instanceof String) confs.add((String) item);
                        }
                        points.add(new RecyclingPoint(
                                pDoc.getId(),
                                name,
                                safe(pDoc.getString("site")),
                                safe(pDoc.getString("block")),
                                safe(pDoc.getString("location")),
                                "pending_report",
                                safe(pDoc.getString("materialsAccepted")),
                                "pending",
                                pDoc.getString("photo"),
                                pDoc.getString("coordinates"),
                                pDoc.getString("userId"),
                                confs
                        ));
                    }
                    showPoints(activeFilter);
                }
            });
        });
    }

    private String safe(String value) {
        return value == null ? "Sin especificar" : value;
    }

    private List<RecyclingPoint> loadOfficialPoints() {
        List<RecyclingPoint> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("recycling_points.json")))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            JSONArray items = new JSONArray(json.toString());
            for (int i = 0; i < items.length(); i++) {
                JSONObject p = items.getJSONObject(i);
                result.add(new RecyclingPoint(
                        p.optString("id", "asset_" + i),
                        p.getString("name"),
                        p.optString("site", ""),
                        "",
                        p.getString("location"),
                        p.optString("bins", ""),
                        p.optString("materials", ""),
                        "approved",
                        null,
                        null,
                        null,
                        new ArrayList<>()
                ));
            }
        } catch (Exception ignored) { }
        return result;
    }

    private void showPoints(@Nullable String filter) {
        container.removeAllViews();
        int visible = 0;
        int approvedCount = 0;
        int pendingCount = 0;

        List<RecyclingPoint> priorityList = new ArrayList<>();
        List<RecyclingPoint> standardList = new ArrayList<>();

        for (RecyclingPoint point : points) {
            if ("approved".equals(point.status)) approvedCount++;
            else if ("pending".equals(point.status)) pendingCount++;

            boolean matches = false;
            if (filter == null) {
                matches = true;
            } else if ("pending".equals(filter)) {
                matches = "pending".equals(point.status);
            } else {
                matches = point.site.toLowerCase().contains(filter)
                        || point.materials.toLowerCase().contains(filter)
                        || point.location.toLowerCase().contains(filter);
            }

            if (matches) {
                if (pointMatchesCategories(point)) {
                    priorityList.add(point);
                } else {
                    standardList.add(point);
                }
                visible++;
            }
        }

        // Primero los puntos que coinciden con las categorías escaneadas
        for (RecyclingPoint p : priorityList) {
            addCard(p, true);
        }
        for (RecyclingPoint p : standardList) {
            addCard(p, false);
        }

        String stats = approvedCount + " aprobados · " + pendingCount + " en validación";
        count.setText(stats);
        empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
        if (visible == 0) {
            empty.setText(points.isEmpty() ? "No hay puntos disponibles." : "No hay puntos que coincidan con este filtro.");
        }
    }

    private void toggleMapFullscreen() {
        isMapFullscreen = !isMapFullscreen;
        if (backCallback != null) backCallback.setEnabled(isMapFullscreen);

        if (btnMapFullscreen != null) {
            btnMapFullscreen.setImageResource(isMapFullscreen ? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBarsVisibility(!isMapFullscreen);
        }

        float targetAlpha = isMapFullscreen ? 0f : 1f;

        if (layoutMapHeader != null) {
            layoutMapHeader.animate().alpha(targetAlpha).setDuration(300).withEndAction(() -> {
                if (isMapFullscreen) layoutMapHeader.setVisibility(View.GONE);
            }).start();
            if (!isMapFullscreen) layoutMapHeader.setVisibility(View.VISIBLE);
        }

        if (layoutMapBottomContent != null) {
            layoutMapBottomContent.animate().alpha(targetAlpha).setDuration(300).withEndAction(() -> {
                if (isMapFullscreen) layoutMapBottomContent.setVisibility(View.GONE);
            }).start();
            if (!isMapFullscreen) layoutMapBottomContent.setVisibility(View.VISIBLE);
        }

        if (mapTemplateContainer != null) {
            int initialHeight = mapTemplateContainer.getHeight();
            int targetHeight = isMapFullscreen ? Math.round(getResources().getDisplayMetrics().heightPixels * 0.70f) : dp(190);

            android.animation.ValueAnimator heightAnim = android.animation.ValueAnimator.ofInt(initialHeight, targetHeight);
            heightAnim.setDuration(300);
            heightAnim.addUpdateListener(anim -> {
                ViewGroup.LayoutParams params = mapTemplateContainer.getLayoutParams();
                params.height = (int) anim.getAnimatedValue();
                mapTemplateContainer.setLayoutParams(params);
            });
            heightAnim.start();
        }
    }

    private boolean pointMatchesCategories(RecyclingPoint p) {
        if (targetCategories.isEmpty()) return false;
        String mat = (p.materials + " " + p.name + " " + p.site).toLowerCase(java.util.Locale.ROOT);
        for (String cat : targetCategories) {
            String c = cat.toLowerCase(java.util.Locale.ROOT);
            if (c.contains("plast") && (mat.contains("plást") || mat.contains("plast") || mat.contains("pet") || mat.contains("botella"))) return true;
            if (c.contains("vidr") && (mat.contains("vidr") || mat.contains("cristal"))) return true;
            if (c.contains("papel") && (mat.contains("papel") || mat.contains("cartón") || mat.contains("carton"))) return true;
            if (c.contains("organ") && (mat.contains("orgán") || mat.contains("organ") || mat.contains("comida"))) return true;
            if (c.contains("metal") && (mat.contains("metal") || mat.contains("lata") || mat.contains("alumin"))) return true;
            if (c.contains("peligr") && (mat.contains("comput") || mat.contains("electr") || mat.contains("bombill") || mat.contains("pila"))) return true;
        }
        return false;
    }

    private void addCard(RecyclingPoint point) {
        addCard(point, false);
    }

    private void addCard(RecyclingPoint point, boolean isHighlighted) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(8);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));

        boolean isPending = "pending".equals(point.status);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_card));
        if (isHighlighted) {
            card.setStrokeColor(0xFF00A859);
            card.setStrokeWidth(dp(2));
        } else {
            card.setStrokeColor(isPending ? 0xFFFFB74D : ContextCompat.getColor(requireContext(), R.color.unisimon_green_light));
        }

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Header: Name + Badge
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView name = text(point.name, 16, R.color.text_primary, true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (isHighlighted) {
            TextView matchBadge = new TextView(requireContext());
            matchBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            matchBadge.setTextSize(9);
            matchBadge.setTypeface(null, 1);
            matchBadge.setText("⭐ Coincide");
            matchBadge.setTextColor(0xFF00562A);
            matchBadge.setBackgroundColor(0xFFDCFCE7);
            LinearLayout.LayoutParams mbParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mbParams.rightMargin = dp(6);
            matchBadge.setLayoutParams(mbParams);
            header.addView(matchBadge);
        }
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = new TextView(requireContext());
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        badge.setTextSize(10);
        badge.setTypeface(null, 1);

        if (isPending) {
            badge.setText("⏳ En validación (" + point.confirmations.size() + "/2)");
            badge.setTextColor(0xFFE65100);
            badge.setBackgroundColor(0xFFFFECB3);
        } else {
            badge.setText("✓ Aprobado");
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_text));
            badge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_recyclable_bg));
        }

        header.addView(name);
        header.addView(badge);
        body.addView(header);

        // Site & location
        String siteInfo = point.block.isEmpty() ? point.site : point.site + " · " + point.block;
        TextView site = text(siteInfo, 12, R.color.unisimon_green, true);
        site.setPadding(0, dp(4), 0, 0);

        TextView location = text(point.location, 13, R.color.text_secondary, false);
        location.setPadding(0, dp(2), 0, dp(4));

        TextView materials = text("Recibe: " + point.materials, 12, R.color.text_primary, false);

        body.addView(site);
        body.addView(location);
        body.addView(materials);

        card.addView(body);

        // Click on card opens PointDetailBottomSheet
        card.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(PointDetailBottomSheetDialogFragment.ARG_ID, point.id);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_NAME, point.name);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_SITE, point.site);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_BLOCK, point.block);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_LOCATION, point.location);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_MATERIALS, point.materials);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_COORDS, point.coords);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_STATUS, point.status);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_PHOTO, point.photo);
            args.putString(PointDetailBottomSheetDialogFragment.ARG_CREATOR_UID, point.creatorUid);
            args.putStringArrayList(PointDetailBottomSheetDialogFragment.ARG_CONFIRMATIONS, point.confirmations);

            PointDetailBottomSheetDialogFragment dialog = PointDetailBottomSheetDialogFragment.newInstance(args);
            dialog.setListener(this);
            dialog.show(getParentFragmentManager(), "point_detail_sheet");
        });

        container.addView(card);
    }

    @Override
    public void onPointConfirmed() {
        reloadAllPoints();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(requireContext(), color));
        if (bold) view.setTypeface(null, 1);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private static class RecyclingPoint {
        final String id, name, site, block, location, bins, materials, status, photo, coords, creatorUid;
        final ArrayList<String> confirmations;

        RecyclingPoint(String id, String name, String site, String block, String location,
                       String bins, String materials, String status, @Nullable String photo,
                       @Nullable String coords, @Nullable String creatorUid, ArrayList<String> confirmations) {
            this.id = id;
            this.name = name;
            this.site = site;
            this.block = block;
            this.location = location;
            this.bins = bins;
            this.materials = materials;
            this.status = status;
            this.photo = photo;
            this.coords = coords;
            this.creatorUid = creatorUid;
            this.confirmations = confirmations;
        }
    }
}
