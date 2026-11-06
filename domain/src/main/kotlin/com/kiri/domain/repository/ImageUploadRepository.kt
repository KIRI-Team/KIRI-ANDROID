package com.kiri.domain.repository

import com.kiri.domain.model.image.UploadImage

// 이미지를 올리고 이미지 키를 돌려준다. 키를 리뷰 작성 같은 API 에 넘기면 업로드가 확정된다
// 업로드 주소는 발급 후 5분만 유효하므로 키는 받은 뒤 바로 쓰는 것이 좋다
interface ImageUploadRepository {
    // 비어 있거나 10MB 를 넘으면 InvalidUploadImageException, 저장소에 올리지 못하면 ImageUploadFailedException,
    // 발급 요청이 거절되면 서버 오류를 그대로, 연결 문제는 IOException 으로 실패한다
    suspend fun upload(image: UploadImage): Result<String>
}
