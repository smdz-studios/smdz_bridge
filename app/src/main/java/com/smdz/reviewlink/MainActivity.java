package com.smdz.reviewlink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(30, 31, 34);
    private static final int PANEL = Color.rgb(43, 45, 49);
    private static final int INPUT = Color.rgb(30, 31, 34);
    private static final int BORDER = Color.rgb(63, 65, 71);
    private static final int TEXT = Color.rgb(242, 243, 245);
    private static final int MUTED = Color.rgb(181, 186, 193);
    private static final int BLUE = Color.rgb(88, 101, 242);
    private static final int SECONDARY = Color.rgb(78, 80, 88);
    private static final int GREEN = Color.rgb(87, 242, 135);
    private static final int RED = Color.rgb(255, 123, 125);

    private EditText mapsInput;
    private Button generateButton;
    private TextView status;
    private LinearLayout resultBox;
    private TextView placeName;
    private TextView placeAddress;
    private EditText reviewOutput;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("review_link_settings", MODE_PRIVATE);
        buildUi();

        if (prefs.getString("api_key", "").trim().isEmpty()) {
            mapsInput.postDelayed(() -> showApiKeyDialog(true), 300);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(18), dp(24), dp(18), dp(24));
        outer.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(outer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(26), dp(22), dp(24));
        card.setBackground(roundRect(PANEL, BORDER, 14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(680));
        outer.addView(card, cardParams);

        TextView google = new TextView(this);
        google.setText(coloredGoogle());
        google.setTextSize(48);
        google.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        google.setGravity(Gravity.CENTER);
        card.addView(google, matchWrap());

        TextView stars = text("★★★★★", 21, Color.rgb(251, 188, 4), Gravity.CENTER);
        LinearLayout.LayoutParams starsParams = matchWrap();
        starsParams.setMargins(0, dp(4), 0, dp(18));
        card.addView(stars, starsParams);

        TextView title = text("Generador de enlace de reseña", 24, TEXT, Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, matchWrap());

        TextView subtitle = text("Genera el enlace directo para que un cliente pueda abrir la pantalla de valoración del negocio.", 14, MUTED, Gravity.CENTER);
        subtitle.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(10), 0, dp(22));
        card.addView(subtitle, subtitleParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), dp(14), dp(14), dp(14));
        info.setBackground(roundRect(Color.rgb(38, 40, 45), BORDER, 10));
        LinearLayout.LayoutParams infoParams = matchWrap();
        infoParams.setMargins(0, 0, 0, dp(20));
        card.addView(info, infoParams);

        TextView infoTitle = text("Importante: usa el enlace de compartir del negocio", 14, TEXT, Gravity.START);
        infoTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        info.addView(infoTitle, matchWrap());

        TextView infoText = text("Abre primero la ficha exacta del negocio en Google Maps y copia el enlace desde el botón Compartir.", 13, MUTED, Gravity.START);
        LinearLayout.LayoutParams infoTextParams = matchWrap();
        infoTextParams.setMargins(0, dp(7), 0, dp(8));
        info.addView(infoText, infoTextParams);

        TextView steps = text("Google Maps → Negocio → Compartir → Copiar enlace → Pégalo aquí", 13, TEXT, Gravity.START);
        steps.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        info.addView(steps, matchWrap());

        TextView inputLabel = text("Enlace del negocio en Google Maps", 13, TEXT, Gravity.START);
        inputLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(inputLabel, matchWrap());

        mapsInput = new EditText(this);
        mapsInput.setSingleLine(true);
        mapsInput.setTextColor(TEXT);
        mapsInput.setHintTextColor(Color.rgb(148, 155, 164));
        mapsInput.setHint("https://maps.app.goo.gl/...");
        mapsInput.setTextSize(14);
        mapsInput.setPadding(dp(14), 0, dp(14), 0);
        mapsInput.setBackground(roundRect(INPUT, BORDER, 8));
        mapsInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        inputParams.setMargins(0, dp(8), 0, 0);
        card.addView(mapsInput, inputParams);

        TextView hint = text("No pegues una búsqueda general. Usa el enlace obtenido desde “Compartir” en la ficha del negocio.", 12, Color.rgb(148, 155, 164), Gravity.START);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.setMargins(0, dp(8), 0, 0);
        card.addView(hint, hintParams);

        generateButton = button("Generar enlace de reseña", BLUE);
        LinearLayout.LayoutParams generateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        generateParams.setMargins(0, dp(15), 0, 0);
        card.addView(generateButton, generateParams);
        generateButton.setOnClickListener(v -> generateReviewLink());

        status = text("", 13, MUTED, Gravity.START);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, dp(11), 0, 0);
        card.addView(status, statusParams);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setVisibility(View.GONE);
        LinearLayout.LayoutParams resultParams = matchWrap();
        resultParams.setMargins(0, dp(10), 0, 0);
        card.addView(resultBox, resultParams);

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.setMargins(0, 0, 0, dp(16));
        resultBox.addView(divider, dividerParams);

        placeName = text("", 17, TEXT, Gravity.START);
        placeName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultBox.addView(placeName, matchWrap());

        placeAddress = text("", 13, MUTED, Gravity.START);
        LinearLayout.LayoutParams addressParams = matchWrap();
        addressParams.setMargins(0, dp(4), 0, dp(15));
        resultBox.addView(placeAddress, addressParams);

        TextView outputLabel = text("Enlace directo de reseña", 13, TEXT, Gravity.START);
        outputLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultBox.addView(outputLabel, matchWrap());

        reviewOutput = new EditText(this);
        reviewOutput.setSingleLine(true);
        reviewOutput.setTextColor(TEXT);
        reviewOutput.setTextSize(13);
        reviewOutput.setPadding(dp(14), 0, dp(14), 0);
        reviewOutput.setBackground(roundRect(INPUT, BORDER, 8));
        reviewOutput.setFocusable(false);
        LinearLayout.LayoutParams reviewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        reviewParams.setMargins(0, dp(8), 0, 0);
        resultBox.addView(reviewOutput, reviewParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonsParams = matchWrap();
        buttonsParams.setMargins(0, dp(8), 0, 0);
        resultBox.addView(buttons, buttonsParams);

        Button copy = button("Copiar", SECONDARY);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(46), 1f);
        half.setMargins(0, 0, dp(4), 0);
        buttons.addView(copy, half);
        copy.setOnClickListener(v -> copyReviewLink());

        Button open = button("Probar enlace", SECONDARY);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        half2.setMargins(dp(4), 0, 0, 0);
        buttons.addView(open, half2);
        open.setOnClickListener(v -> openReviewLink());

        Button apiSettings = button("Cambiar API key", Color.rgb(54, 56, 62));
        LinearLayout.LayoutParams apiParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        apiParams.setMargins(0, dp(18), 0, 0);
        card.addView(apiSettings, apiParams);
        apiSettings.setOnClickListener(v -> showApiKeyDialog(false));

        TextView footer = text("Uso privado · La API key se guarda únicamente en este dispositivo", 11, Color.rgb(148, 155, 164), Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.setMargins(0, dp(16), 0, 0);
        card.addView(footer, footerParams);

        setContentView(scroll);
    }

    private void showApiKeyDialog(boolean required) {
        final EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint("AIza...");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setText(prefs.getString("api_key", ""));
        keyInput.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("API key de Google Places")
                .setMessage("Introduce tu API key. Se guardará solo en este móvil y podrás cambiarla después.")
                .setView(keyInput)
                .setCancelable(!required)
                .setNegativeButton(required ? null : "Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();
            if (key.isEmpty()) {
                keyInput.setError("Introduce la API key");
                return;
            }
            prefs.edit().putString("api_key", key).apply();
            Toast.makeText(this, "API key guardada", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void generateReviewLink() {
        String apiKey = prefs.getString("api_key", "").trim();
        if (apiKey.isEmpty()) {
            showApiKeyDialog(true);
            return;
        }

        String input = mapsInput.getText().toString().trim();
        if (input.isEmpty()) {
            setStatus("Pega primero el enlace compartido del negocio.", RED);
            return;
        }

        resultBox.setVisibility(View.GONE);
        generateButton.setEnabled(false);
        generateButton.setText("Generando...");
        setStatus("Buscando el negocio...", MUTED);

        new Thread(() -> {
            try {
                PlaceResult result = resolvePlace(input, apiKey);
                runOnUiThread(() -> {
                    placeName.setText(result.name);
                    placeAddress.setText(result.address);
                    reviewOutput.setText(result.reviewUrl);
                    resultBox.setVisibility(View.VISIBLE);
                    setStatus("Enlace generado correctamente.", GREEN);
                    resetGenerateButton();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setStatus(e.getMessage() == null ? "No se pudo generar el enlace." : e.getMessage(), RED);
                    resetGenerateButton();
                });
            }
        }).start();
    }

    private PlaceResult resolvePlace(String raw, String apiKey) throws Exception {
        String input = raw.trim();
        if (looksLikePlaceId(input)) {
            return getPlaceDetails(input, apiKey);
        }

        if (!input.startsWith("https://") && !input.startsWith("http://")) {
            throw new Exception("Introduce un enlace válido de Google Maps.");
        }

        String resolved = resolveRedirect(input);
        Uri uri = Uri.parse(resolved);
        String placeId = firstNonEmpty(uri.getQueryParameter("query_place_id"), uri.getQueryParameter("place_id"), uri.getQueryParameter("placeid"));
        if (placeId != null) {
            return getPlaceDetails(placeId, apiKey);
        }

        String query = firstNonEmpty(uri.getQueryParameter("query"), uri.getQueryParameter("q"), uri.getQueryParameter("destination"));
        if (query == null || query.trim().isEmpty()) {
            query = extractPlaceName(uri.getEncodedPath());
        }

        if (query == null || query.trim().isEmpty()) {
            throw new Exception("No pude identificar el negocio. Abre su ficha exacta en Google Maps, pulsa Compartir y copia ese enlace.");
        }

        return searchPlace(query.trim(), apiKey);
    }

    private PlaceResult searchPlace(String query, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://places.googleapis.com/v1/places:searchText").openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-Goog-Api-Key", apiKey);
        connection.setRequestProperty("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.googleMapsLinks");

        JSONObject body = new JSONObject();
        body.put("textQuery", query);
        body.put("languageCode", "es");
        body.put("regionCode", "ES");
        body.put("maxResultCount", 1);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        JSONObject json = readJson(connection);
        JSONArray places = json.optJSONArray("places");
        if (places == null || places.length() == 0) {
            throw new Exception("Google no encontró ese negocio. Prueba de nuevo con el enlace obtenido desde Compartir.");
        }
        return parsePlace(places.getJSONObject(0));
    }

    private PlaceResult getPlaceDetails(String placeId, String apiKey) throws Exception {
        URL url = new URL("https://places.googleapis.com/v1/places/" + Uri.encode(placeId) + "?languageCode=es");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("X-Goog-Api-Key", apiKey);
        connection.setRequestProperty("X-Goog-FieldMask", "id,displayName,formattedAddress,googleMapsLinks");
        return parsePlace(readJson(connection));
    }

    private PlaceResult parsePlace(JSONObject place) throws Exception {
        JSONObject links = place.optJSONObject("googleMapsLinks");
        String reviewUrl = links == null ? "" : links.optString("writeAReviewUri", "");
        if (reviewUrl.isEmpty()) {
            throw new Exception("Google encontró el negocio, pero no devolvió un enlace directo para escribir una reseña.");
        }

        JSONObject displayName = place.optJSONObject("displayName");
        String name = displayName == null ? "Negocio encontrado" : displayName.optString("text", "Negocio encontrado");
        String address = place.optString("formattedAddress", "");
        return new PlaceResult(name, address, reviewUrl);
    }

    private JSONObject readJson(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
        }
        JSONObject json = sb.length() == 0 ? new JSONObject() : new JSONObject(sb.toString());
        if (code < 200 || code >= 300) {
            JSONObject error = json.optJSONObject("error");
            String message = error == null ? "Google rechazó la solicitud (HTTP " + code + ")." : error.optString("message", "Google rechazó la solicitud.");
            throw new Exception(message);
        }
        return json;
    }

    private String resolveRedirect(String input) throws Exception {
        URL current = new URL(input);
        for (int i = 0; i < 6; i++) {
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GoogleReviewLink/1.0");
            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                if (location == null || location.isEmpty()) break;
                current = new URL(current, location);
                continue;
            }
            return connection.getURL().toString();
        }
        return current.toString();
    }

    private String extractPlaceName(String encodedPath) {
        if (encodedPath == null) return null;
        String[] parts = encodedPath.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("place".equals(parts[i]) && !parts[i + 1].isEmpty()) {
                try {
                    return URLDecoder.decode(parts[i + 1].replace("+", " "), "UTF-8");
                } catch (Exception ignored) {
                    return parts[i + 1].replace("+", " ");
                }
            }
        }
        return null;
    }

    private boolean looksLikePlaceId(String value) {
        return value.length() >= 20 && value.matches("[A-Za-z0-9_-]+") && !value.contains(".");
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private void copyReviewLink() {
        String link = reviewOutput.getText().toString().trim();
        if (link.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Enlace de reseña", link));
        Toast.makeText(this, "Enlace copiado", Toast.LENGTH_SHORT).show();
    }

    private void openReviewLink() {
        String link = reviewOutput.getText().toString().trim();
        if (link.isEmpty()) return;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }

    private void setStatus(String message, int color) {
        status.setText(message);
        status.setTextColor(color);
    }

    private void resetGenerateButton() {
        generateButton.setEnabled(true);
        generateButton.setText("Generar enlace de reseña");
    }

    private SpannableString coloredGoogle() {
        SpannableString s = new SpannableString("Google");
        int[] colors = {
                Color.rgb(66, 133, 244),
                Color.rgb(234, 67, 53),
                Color.rgb(251, 188, 5),
                Color.rgb(66, 133, 244),
                Color.rgb(52, 168, 83),
                Color.rgb(234, 67, 53)
        };
        for (int i = 0; i < colors.length; i++) {
            s.setSpan(new ForegroundColorSpan(colors[i]), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private TextView text(String value, float size, int color, int gravity) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(gravity);
        return view;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundRect(color, color, 8));
        return button;
    }

    private GradientDrawable roundRect(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class PlaceResult {
        final String name;
        final String address;
        final String reviewUrl;

        PlaceResult(String name, String address, String reviewUrl) {
            this.name = name;
            this.address = address;
            this.reviewUrl = reviewUrl;
        }
    }
}
