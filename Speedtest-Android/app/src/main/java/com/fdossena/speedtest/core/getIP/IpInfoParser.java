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

            return processed
                    +"\n"+LOCATION_LABEL+" "+parsed.place
                    +"\n"+COORDINATES_LABEL+" "+parsed.latitude+", "+parsed.longitude;
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

        Double latitude=readCoordinate(location,"latitude","lat");
        Double longitude=readCoordinate(location,"longitude","lon","lng");
        if(latitude==null||longitude==null){
            JSONObject coordinates=location.optJSONObject("coordinates");
            if(coordinates!=null){
                if(latitude==null) latitude=readCoordinate(coordinates,"latitude","lat");
                if(longitude==null) longitude=readCoordinate(coordinates,"longitude","lon","lng");
            }
        }
        if(latitude==null||longitude==null){
            Object locValue=location.opt("loc");
            if(locValue instanceof String){
                String loc=cleanText(locValue);
                String[] parts=loc.split(",",-1);
                if(parts.length>=2){
                    if(latitude==null) latitude=parseCoordinate(parts[0].trim());
                    if(longitude==null) longitude=parseCoordinate(parts[1].trim());
                }
            }
        }

        if(city.isEmpty()||region.isEmpty()||(countryName.isEmpty()&&country.isEmpty())) return null;
        if(place.isEmpty()||latitude==null||longitude==null) return null;
        if(latitude< -90||latitude>90||longitude< -180||longitude>180) return null;
        if(Double.isNaN(latitude)||Double.isNaN(longitude)||Double.isInfinite(latitude)||Double.isInfinite(longitude)) return null;

        return new LocationInfo(place, formatCoordinate(latitude), formatCoordinate(longitude));
    }

    private static Double readCoordinate(JSONObject object,String... names){
        for(String name:names){
            if(object.has(name)){
                Double coordinate=parseCoordinate(object.opt(name));
                if(coordinate!=null) return coordinate;
            }
        }
        return null;
    }

    private static Double parseCoordinate(Object value){
        if(value instanceof Number){
            double coordinate=((Number)value).doubleValue();
            return Double.isNaN(coordinate)||Double.isInfinite(coordinate)?null:coordinate;
        }
        if(value instanceof String){
            try{
                double coordinate=Double.parseDouble(((String)value).trim());
                return Double.isNaN(coordinate)||Double.isInfinite(coordinate)?null:coordinate;
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
