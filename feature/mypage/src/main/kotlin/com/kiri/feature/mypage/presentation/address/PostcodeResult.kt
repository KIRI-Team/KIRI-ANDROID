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
<!doctype html><html lang="ko"><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0"><div id="picker" style="width:100%;height:100vh"></div>
<script>
function reportError(){window.location.href='kiri-address://error';}
function openPicker(){try{
  var Postcode=(window.kakao&&kakao.Postcode)||(window.daum&&daum.Postcode);
  if(!Postcode){reportError();return;}
  new Postcode({oncomplete:function(d){
    var address=d.userSelectedType==='R'?(d.roadAddress||d.autoRoadAddress):(d.jibunAddress||d.autoJibunAddress);
    address=address||d.roadAddress||d.jibunAddress||'';
    window.location.href='kiri-address://selected?zip='+encodeURIComponent(d.zonecode||'')+
      '&address='+encodeURIComponent(address)+'&region='+encodeURIComponent(d.bcode||'');
  },width:'100%',height:'100%'}).embed(document.getElementById('picker'),{autoClose:false});
  window.location.href='kiri-address://ready';
}catch(e){reportError();}}
</script>
<script src="https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js" onload="openPicker()" onerror="reportError()"></script>
</body></html>
""".trimIndent()
