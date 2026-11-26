package com.fdossena.speedtest.core.getIP;

import org.json.JSONObject;

public final class IpInfoParser {
    public static final String LOCATION_LABEL = "Approximate IP-derived location:";
    public static final String COORDINATES_LABEL = "Latitude/longitude:";

    private IpInfoParser(){}

    public static String formatDisplayString(String payload){
        if(payload==null) return "";
        try{
            JSONObject json=new JSONObject(payload);
            if(!json.has("processedString")) return payload;
            Object value=json.opt("processedString");
            if(!(value instanceof String)) return payload;
            String processed=(String)value;
            if(processed.isEmpty()) return processed;

            JSONObject location=json.optJSONObject("location");
            if(location==null) return processed;
            LocationInfo parsed=parseLocation(location);
            if(parsed==null) return processed;

            StringBuilder display=new StringBuilder(processed);
            if(!parsed.place.isEmpty()){
                display.append("\n").append(LOCATION_LABEL).append(" ").append(parsed.place);
            }
            if(!parsed.latitude.isEmpty()&&!parsed.longitude.isEmpty()){
                display.append("\n").append(COORDINATES_LABEL).append(" ").append(parsed.latitude).append(", ").append(parsed.longitude);
            }
            return parsed.place.isEmpty()&&parsed.latitude.isEmpty()&&parsed.longitude.isEmpty()?processed:display.toString();
        }catch(Throwable t){
            return payload;
        }
    }

    private static LocationInfo parseLocation(JSONObject location){
        String city=cleanText(readText(location,"city"));
        String region=cleanText(readText(location,"region"));
        String countryName=cleanText(readText(location,"countryName","country_name"));
        String country=cleanText(readText(location,"country","countryCode","country_code"));
        String place=formatPlace(city,region,countryName.isEmpty()?country:countryName);

        CoordinatePair coordinates=readCoordinatePair(location);

        if(place.isEmpty()&&coordinates==null) return null;
        return new LocationInfo(
                place,
                coordinates==null?"":formatCoordinate(coordinates.latitude),
                coordinates==null?"":formatCoordinate(coordinates.longitude)
        );
    }

    private static CoordinatePair readCoordinatePair(JSONObject location){
        CoordinatePair pair=readCoordinatePairFromObject(location);
        if(pair!=null) return pair;
        JSONObject coordinates=location.optJSONObject("coordinates");
        if(coordinates!=null){
            pair=readCoordinatePairFromObject(coordinates);
            if(pair!=null) return pair;
            pair=readCoordinatePairFromLoc(coordinates.opt("loc"));
            if(pair!=null) return pair;
        }
        return readCoordinatePairFromLoc(location.opt("loc"));
    }

    private static CoordinatePair readCoordinatePairFromObject(JSONObject object){
        if(object==null) return null;
        CoordinatePair pair=readCoordinatePair(
                object.opt("latitude"),object.opt("longitude"),-90.0,90.0,-180.0,180.0);
        if(pair!=null) return pair;
        pair=readCoordinatePair(object.opt("lat"),object.opt("lon"),-90.0,90.0,-180.0,180.0);
        if(pair!=null) return pair;
        pair=readCoordinatePair(object.opt("lat"),object.opt("lng"),-90.0,90.0,-180.0,180.0);
        if(pair!=null) return pair;
        pair=readCoordinatePair(object.opt("latitude"),object.opt("lon"),-90.0,90.0,-180.0,180.0);
        if(pair!=null) return pair;
        pair=readCoordinatePair(object.opt("latitude"),object.opt("lng"),-90.0,90.0,-180.0,180.0);
        if(pair!=null) return pair;
        return readCoordinatePair(object.opt("lat"),object.opt("longitude"),-90.0,90.0,-180.0,180.0);
    }

    private static CoordinatePair readCoordinatePair(Object latitudeValue,Object longitudeValue,
                                                     double latitudeMinimum,double latitudeMaximum,
                                                     double longitudeMinimum,double longitudeMaximum){
        Double latitude=parseCoordinate(latitudeValue,latitudeMinimum,latitudeMaximum);
        Double longitude=parseCoordinate(longitudeValue,longitudeMinimum,longitudeMaximum);
        return latitude==null||longitude==null?null:new CoordinatePair(latitude,longitude);
    }

    private static CoordinatePair readCoordinatePairFromLoc(Object locValue){
        String[] parts=splitLoc(locValue);
        if(parts==null) return null;
        return readCoordinatePair(parts[0],parts[1],-90.0,90.0,-180.0,180.0);
    }

    private static String[] splitLoc(Object locValue){
        if(!(locValue instanceof String)) return null;
        String loc=cleanText(locValue);
        if(loc.isEmpty()) return null;
        String[] parts=loc.split(",",-1);
        return parts.length>=2?parts:null;
    }

    private static Double parseCoordinate(Object value,double minimum,double maximum){
        if(value instanceof Number){
            double coordinate=((Number)value).doubleValue();
            if(Double.isNaN(coordinate)||Double.isInfinite(coordinate)) return null;
            return coordinate>=minimum&&coordinate<=maximum?coordinate:null;
        }
        if(value instanceof String){
            try{
                double coordinate=Double.parseDouble(((String)value).trim());
                if(Double.isNaN(coordinate)||Double.isInfinite(coordinate)) return null;
                return coordinate>=minimum&&coordinate<=maximum?coordinate:null;
            }catch(NumberFormatException ignored){}
        }
        return null;
    }

    private static String readText(JSONObject object,String... names){
        for(String name:names){
            if(object.has(name)){
                String value=cleanText(object.opt(name));
                if(!value.isEmpty()) return value;
            }
        }
        return "";
    }

    private static String cleanText(Object value){
        if(!(value instanceof String)) return "";
        String text=((String)value).trim();
        if(text.isEmpty()) return "";
        StringBuilder cleaned=new StringBuilder(text.length());
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c=='\n'||c=='\r'||c=='\t'||Character.isISOControl(c)||c=='\u2028'||c=='\u2029'){
                cleaned.append(' ');
            }else{
                cleaned.append(c);
            }
        }
        return cleaned.toString().trim().replaceAll("\\s+"," ");
    }

    private static String formatPlace(String city,String region,String country){
        StringBuilder place=new StringBuilder();
        appendPlacePart(place,city);
        appendPlacePart(place,region);
        appendPlacePart(place,country);
        return place.toString();
    }

    private static void appendPlacePart(StringBuilder place,String part){
        if(part==null||part.isEmpty()) return;
        for(String existing:place.toString().split(", ")) {
            if(existing.equalsIgnoreCase(part)) return;
        }
        if(place.length()>0) place.append(", ");
        place.append(part);
    }

    private static String formatCoordinate(Double coordinate){
        if(coordinate==0.0) return "0";
        return Double.toString(coordinate);
    }

    private static final class CoordinatePair {
        private final Double latitude;
        private final Double longitude;

        private CoordinatePair(Double latitude,Double longitude){
            this.latitude=latitude;
            this.longitude=longitude;
        }
    }

    private static final class LocationInfo {
        private final String place;
        private final String latitude;
        private final String longitude;

        private LocationInfo(String place,String latitude,String longitude){
            this.place=place;
            this.latitude=latitude;
            this.longitude=longitude;
        }
    }
}
