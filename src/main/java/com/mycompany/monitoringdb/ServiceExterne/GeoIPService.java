/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.ServiceExterne;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 *
 * @author Fredos
 */
public class GeoIPService {

    private static final String GEOIP_API_URL = "http://ip-api.com/json/";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Récupère les informations de géolocalisation d'une adresse IP
     */
    public static GeoIPInfo getGeoIPInfo(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "localhost".equalsIgnoreCase(ipAddress)) {
            return new GeoIPInfo(ipAddress, "", "", "", "", "Local", false);
        }

        try {
            URL url = new URL(GEOIP_API_URL + ipAddress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return new GeoIPInfo(ipAddress, "N/A", "N/A", "N/A", "N/A", "Erreur API", false);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonNode json = mapper.readTree(response.toString());

                if (json.has("status") && "fail".equals(json.get("status").asText())) {
                    return new GeoIPInfo(ipAddress, "N/A", "N/A", "N/A", "N/A", "IP non géolocalisable", false);
                }

                String country = json.has("country") ? json.get("country").asText() : "N/A";
                String city = json.has("city") ? json.get("city").asText() : "N/A";
                String region = json.has("regionName") ? json.get("regionName").asText() : "N/A";
                String isp = json.has("isp") ? json.get("isp").asText() : "N/A";
                String org = json.has("org") ? json.get("org").asText() : "N/A";
                double lat = json.has("lat") ? json.get("lat").asDouble() : 0.0;
                double lon = json.has("lon") ? json.get("lon").asDouble() : 0.0;

                return new GeoIPInfo(ipAddress, country, city, region, isp, org, lat, lon, true);
            }

        } catch (Exception e) {
            System.err.println("Erreur géolocalisation IP " + ipAddress + " : " + e.getMessage());
            return new GeoIPInfo(ipAddress, "N/A", "N/A", "N/A", "N/A", "Erreur: " + e.getMessage(), false);
        }
    }

    /**
     * Vérifie si une IP est suspecte (externe au réseau local)
     */
    public static boolean isSuspiciousIP(String ipAddress) {
        if (ipAddress == null) return false;

        // IP locales
        if (ipAddress.startsWith("192.168.") ||
            ipAddress.startsWith("10.") ||
            ipAddress.startsWith("172.16.") ||
            ipAddress.startsWith("172.17.") ||
            ipAddress.startsWith("172.18.") ||
            ipAddress.startsWith("172.19.") ||
            ipAddress.startsWith("172.20.") ||
            ipAddress.startsWith("172.21.") ||
            ipAddress.startsWith("172.22.") ||
            ipAddress.startsWith("172.23.") ||
            ipAddress.startsWith("172.24.") ||
            ipAddress.startsWith("172.25.") ||
            ipAddress.startsWith("172.26.") ||
            ipAddress.startsWith("172.27.") ||
            ipAddress.startsWith("172.28.") ||
            ipAddress.startsWith("172.29.") ||
            ipAddress.startsWith("172.30.") ||
            ipAddress.startsWith("172.31.") ||
            ipAddress.startsWith("127.") ||
            "localhost".equalsIgnoreCase(ipAddress) ||
            "0:0:0:0:0:0:0:1".equals(ipAddress) ||
            "::1".equals(ipAddress)) {
            return false; // IP locale, non suspecte
        }

        // IP publique → suspecte
        return true;
    }

    /**
     * Classe pour stocker les informations de géolocalisation
     */
    public static class GeoIPInfo {
        private final String ipAddress;
        private final String country;
        private final String city;
        private final String region;
        private final String isp;
        private final String organization;
        private final double latitude;
        private final double longitude;
        private final boolean success;

        public GeoIPInfo(String ipAddress, String country, String city, String region,
                         String isp, String org, double lat, double lon, boolean success) {
            this.ipAddress = ipAddress;
            this.country = country;
            this.city = city;
            this.region = region;
            this.isp = isp;
            this.organization = org;
            this.latitude = lat;
            this.longitude = lon;
            this.success = success;
        }

        public GeoIPInfo(String ipAddress, String country, String city, String region,
                         String isp, String org, boolean success) {
            this(ipAddress, country, city, region, isp, org, 0.0, 0.0, success);
        }

        public String getIpAddress() { return ipAddress; }
        public String getCountry() { return country; }
        public String getCity() { return city; }
        public String getRegion() { return region; }
        public String getIsp() { return isp; }
        public String getOrganization() { return organization; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public boolean isSuccess() { return success; }
        public boolean isSuspicious() { return isSuspiciousIP(ipAddress); }

        @Override
        public String toString() {
            if (!success) {
                return "🌐 " + ipAddress + " (non géolocalisé)";
            }
            return String.format("🌐 %s - %s, %s (%s) - %s [%.4f, %.4f]",
                    ipAddress, city, country, region, isp, latitude, longitude);
        }

        public String toShortString() {
            if (!success) return ipAddress;
            return String.format("%s, %s (%s)", city, country, isp);
        }
    }
}