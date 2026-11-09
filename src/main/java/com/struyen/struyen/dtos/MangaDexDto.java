package com.struyen.struyen.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class MangaDexDto {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaListResponse {
        private String result;
        private String response;
        private List<MangaData> data;
        private Integer limit;
        private Integer offset;
        private Integer total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaData {
        private String id;
        private String type;
        private MangaAttributes attributes;
        private List<Relationship> relationships;

        public String getCoverFileName() {
            if (relationships == null) return null;
            for (Relationship rel : relationships) {
                if ("cover_art".equalsIgnoreCase(rel.getType()) && rel.getAttributes() != null) {
                    Object fileNameObj = rel.getAttributes().get("fileName");
                    if (fileNameObj != null) {
                        return fileNameObj.toString();
                    }
                }
            }
            return null;
        }

        public String getAuthorName() {
            if (relationships == null) return null;
            for (Relationship rel : relationships) {
                if ("author".equalsIgnoreCase(rel.getType()) && rel.getAttributes() != null) {
                    Object nameObj = rel.getAttributes().get("name");
                    if (nameObj != null) {
                        return nameObj.toString();
                    }
                }
            }
            return null;
        }

        public String getPreferredTitle() {
            if (attributes == null) return "Untitled";

            // 1. Uu tien tim tieu de Tieng Viet trong altTitles
            if (attributes.getAltTitles() != null) {
                for (Map<String, String> alt : attributes.getAltTitles()) {
                    if (alt.containsKey("vi") && alt.get("vi") != null && !alt.get("vi").isBlank()) {
                        return alt.get("vi");
                    }
                }
            }

            // 2. Tim tieu de Tieng Viet trong title map
            if (attributes.getTitle() != null) {
                if (attributes.getTitle().containsKey("vi") && !attributes.getTitle().get("vi").isBlank()) {
                    return attributes.getTitle().get("vi");
                }
                // 3. Fallback sang tieng Anh hoac bat ky tieu de mac dinh nao
                if (attributes.getTitle().containsKey("en") && !attributes.getTitle().get("en").isBlank()) {
                    return attributes.getTitle().get("en");
                }
                for (String val : attributes.getTitle().values()) {
                    if (val != null && !val.isBlank()) {
                        return val;
                    }
                }
            }

            // 4. Fallback sang altTitle dau tien co the dung
            if (attributes.getAltTitles() != null) {
                for (Map<String, String> alt : attributes.getAltTitles()) {
                    for (String val : alt.values()) {
                        if (val != null && !val.isBlank()) {
                            return val;
                        }
                    }
                }
            }

            return "Untitled";
        }

        public String getPreferredDescription() {
            if (attributes == null || attributes.getDescription() == null) {
                return "";
            }
            if (attributes.getDescription().containsKey("vi") && !attributes.getDescription().get("vi").isBlank()) {
                return attributes.getDescription().get("vi");
            }
            if (attributes.getDescription().containsKey("en") && !attributes.getDescription().get("en").isBlank()) {
                return attributes.getDescription().get("en");
            }
            for (String desc : attributes.getDescription().values()) {
                if (desc != null && !desc.isBlank()) {
                    return desc;
                }
            }
            return "";
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MangaAttributes {
        private Map<String, String> title;
        private List<Map<String, String>> altTitles;
        private Map<String, String> description;
        private String status;
        private String originalLanguage;
        private String updatedAt;
        private List<Tag> tags;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private String id;
        private String type;
        private TagAttributes attributes;

        public String getTagName() {
            if (attributes != null && attributes.getName() != null) {
                return attributes.getName().get("en");
            }
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagAttributes {
        private Map<String, String> name;
        private String group;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Relationship {
        private String id;
        private String type;
        private String related;
        private Map<String, Object> attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterListResponse {
        private String result;
        private String response;
        private List<ChapterData> data;
        private Integer limit;
        private Integer offset;
        private Integer total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterData {
        private String id;
        private String type;
        private ChapterAttributes attributes;
        private List<Relationship> relationships;

        public String getScanlationGroupName() {
            if (relationships == null) return null;
            for (Relationship rel : relationships) {
                if ("scanlation_group".equalsIgnoreCase(rel.getType()) && rel.getAttributes() != null) {
                    Object nameObj = rel.getAttributes().get("name");
                    if (nameObj != null) {
                        return nameObj.toString();
                    }
                }
            }
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterAttributes {
        private String volume;
        private String chapter;
        private String title;
        private String translatedLanguage;
        private String externalUrl;
        private String publishAt;
        private Integer pages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtHomeResponse {
        private String result;
        private String baseUrl;
        private AtHomeChapter chapter;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtHomeChapter {
        private String hash;
        private List<String> data;
        private List<String> dataSaver;
    }
}

