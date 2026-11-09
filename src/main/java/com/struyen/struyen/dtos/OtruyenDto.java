package com.struyen.struyen.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public class OtruyenDto {

    @Data
    public static class BaseResponse<T> {
        private String status;
        private String message;
        private T data;
    }

    @Data
    public static class ListData {
        @JsonProperty("APP_DOMAIN_CDN_IMAGE")
        private String appDomainCdnImage;
        private List<StoryItem> items;
        private Pagination pagination;
    }

    @Data
    public static class DetailData {
        @JsonProperty("APP_DOMAIN_CDN_IMAGE")
        private String appDomainCdnImage;
        private StoryItem item;
    }

    @Data
    public static class StoryItem {
        @JsonProperty("_id")
        private String id;
        private String name;
        private String slug;
        private String content; // html description
        private String status;
        @JsonProperty("thumb_url")
        private String thumbUrl;
        private List<String> author;
        private List<CategoryItem> category;
        private List<ChapterServer> chapters;
        private String updatedAt;
    }

    @Data
    public static class CategoryItem {
        private String id;
        private String name;
        private String slug;
    }

    @Data
    public static class ChapterServer {
        @JsonProperty("server_name")
        private String serverName;
        @JsonProperty("server_data")
        private List<ChapterItem> serverData;
    }

    @Data
    public static class ChapterItem {
        private String filename;
        @JsonProperty("chapter_name")
        private String chapterName;
        @JsonProperty("chapter_title")
        private String chapterTitle;
        @JsonProperty("chapter_api_data")
        private String chapterApiData;
    }

    @Data
    public static class Pagination {
        private int totalItems;
        private int totalItemsPerPage;
        private int currentPage;
        private int totalPages;
    }
    
    // For chapter images
    @Data
    public static class ChapterDetailData {
        private String domain_cdn;
        private ChapterDetailItem item;
    }

    @Data
    public static class ChapterDetailItem {
        private String _id;
        private String comic_name;
        private String chapter_name;
        private String chapter_title;
        private String chapter_path;
        private List<ChapterImage> chapter_image;
    }

    @Data
    public static class ChapterImage {
        private String image_file;
        private int image_page;
    }
}
