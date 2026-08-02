package com.ject.vs.admin.adapter.web.dto;

import com.ject.vs.vote.domain.VoteDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 어드민 투표 생성 폼 바인딩 객체.
 * 이미지 입력은 S3 파일 업로드 / URL 직접 입력 두 가지를 지원한다.
 */
@Getter
@Setter
public class AdminVoteForm {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    private String content;

    @NotNull(message = "진행 기간을 선택해주세요.")
    private VoteDuration duration = VoteDuration.HOURS_24;

    @NotBlank(message = "선택지 A를 입력해주세요.")
    @Size(max = 50, message = "선택지는 50자 이하여야 합니다.")
    private String optionA;

    @NotBlank(message = "선택지 B를 입력해주세요.")
    @Size(max = 50, message = "선택지는 50자 이하여야 합니다.")
    private String optionB;

    private ImageSource imageSource = ImageSource.FILE;

    private MultipartFile thumbnailFile;
    private MultipartFile imageFile;

    @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")
    private String thumbnailUrl;

    @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")
    private String imageUrl;

    public boolean usesFileUpload() {
        return imageSource != ImageSource.URL;
    }

    public enum ImageSource {
        /** 파일을 업로드하면 서버가 S3에 올리고 URL을 만들어준다. */
        FILE,
        /** 이미 올라간 이미지의 URL을 그대로 사용한다. */
        URL
    }
}
