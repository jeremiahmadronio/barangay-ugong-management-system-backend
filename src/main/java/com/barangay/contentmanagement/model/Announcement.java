package com.barangay.contentmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cms_announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(name = "category_color")
    private String categoryColor;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "image_url")
    private String imageUrl;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_published")
    private boolean isPublished = true;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_announcement_images", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "image_url")
    private List<String> additionalImages = new ArrayList<>();
}
