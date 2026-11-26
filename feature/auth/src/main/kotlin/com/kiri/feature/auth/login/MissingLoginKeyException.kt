package com.kiri.feature.auth.login

// local.properties 에 로그인 키를 넣지 않은 채 로그인하려 할 때
class MissingLoginKeyException(keyName: String) :
    IllegalStateException("$keyName 이(가) 설정되지 않았습니다. local.properties 를 확인하세요.")
