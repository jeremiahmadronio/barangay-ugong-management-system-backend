package com.barangay.ftjs.dto;


import com.barangay.zshared.enumerated.TimeLineType;
import java.time.LocalDateTime;

public record FtjsTimelineDto(
        Long id,
        String title,
        String description,
        TimeLineType timelineType,
        LocalDateTime eventDate,
        String actorName
) {}