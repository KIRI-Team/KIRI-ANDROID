package com.kiri.feature.mypage.presentation.address

import com.kiri.feature.mypage.model.address.HomeAddressUiEvent
import java.net.URI
import java.net.URLDecoder

/** 웹 페이지의 완료 콜백만 받는다. 다른 URL·잘린 결과·중복 필드는 선택으로 처리하지 않는다. */
internal fun parsePostcodeSelection(url: String): HomeAddressUiEvent.AddressSelected? = runCatching {
    require(url.length <= 4096)
    val uri = URI(url)
    require(uri.scheme == "kiri-address" && uri.host == "selected" && uri.userInfo == null && uri.port == -1)
    require(uri.path.isNullOrEmpty() && uri.fragment == null)
    val fields = uri.rawQuery.orEmpty().split('&').map { pair ->
        val parts = pair.split('=', limit = 2)
        require(parts.size == 2)
        URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8").trim()
    }
    require(fields.size == 3 && fields.map { it.first }.toSet() == setOf("zip", "address", "region"))
    val values = fields.toMap()
    val zip = values.getValue("zip")
    val address = values.getValue("address")
    val region = values.getValue("region")
    require(zip.matches(Regex("[0-9]{5}")) && region.matches(Regex("[0-9]{10}")))
    require(address.length in 1..300 && address.none { it.isISOControl() })
    HomeAddressUiEvent.AddressSelected(zip, address, region)
}.getOrNull()

internal val postcodePage = """
<!doctype html><html lang="ko"><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0;padding:0;width:100%;height:100%;background:#1A1A1A}#picker{width:100%;height:100%}</style></head>
<body><div id="picker"></div>
<script>
// WebView 의 레이아웃 높이가 늦게 정해지면 화면 높이 단위(vh)나 100% 가 0 으로 계산되어 검색 화면이 흰 화면으로 보인다.
// 그래서 실제 화면 높이(innerHeight)로 직접 맞추고, 크기가 바뀔 때마다 다시 맞춘다
function fitPicker(){var p=document.getElementById('picker');if(p&&window.innerHeight>0)p.style.height=window.innerHeight+'px';}
window.addEventListener('resize',fitPicker);fitPicker();
// 앱 색(KiriColor)에 맞춘 우편번호 서비스 테마. 카카오가 열어 둔 색 옵션만 바꾼다
var POSTCODE_THEME={
  bgColor:'#1A1A1A',          // 바탕 (Bg.Base)
  searchBgColor:'#252524',    // 검색창 바탕 (Bg.Primary)
  contentBgColor:'#1A1A1A',   // 검색 결과·안내 바탕 (Bg.Base)
  pageBgColor:'#252524',      // 하단 페이지 바탕 (Bg.Primary)
  textColor:'#D9D9D8',        // 기본 글자 (Text.Primary)
  queryTextColor:'#F7F7F6',   // 검색어 글자 (Text.Strong)
  postcodeTextColor:'#9BEA51',// 우편번호 (Text.Accent)
  emphTextColor:'#9BEA51',    // 강조 글자 (Text.Accent)
  outlineColor:'#404140'      // 테두리 (Border.Default)
};
function reportError(){window.location.href='kiri-address://error';}
function openPicker(){try{
  fitPicker();
  var Postcode=(window.kakao&&kakao.Postcode)||(window.daum&&daum.Postcode);
  if(!Postcode){reportError();return;}
  new Postcode({oncomplete:function(d){
    var address=d.userSelectedType==='R'?(d.roadAddress||d.autoRoadAddress):(d.jibunAddress||d.autoJibunAddress);
    address=address||d.roadAddress||d.jibunAddress||'';
    window.location.href='kiri-address://selected?zip='+encodeURIComponent(d.zonecode||'')+
      '&address='+encodeURIComponent(address)+'&region='+encodeURIComponent(d.bcode||'');
  },width:'100%',height:'100%',theme:POSTCODE_THEME}).embed(document.getElementById('picker'),{autoClose:false});
  window.location.href='kiri-address://ready';
}catch(e){reportError();}}
</script>
<script src="https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js" onload="openPicker()" onerror="reportError()"></script>
</body></html>
""".trimIndent()
