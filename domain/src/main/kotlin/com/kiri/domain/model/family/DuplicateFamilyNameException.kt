package com.kiri.domain.model.family

// 같은 이름의 가족 공간이 이미 있다
class DuplicateFamilyNameException : Exception("이미 존재하는 가족 이름입니다.")
