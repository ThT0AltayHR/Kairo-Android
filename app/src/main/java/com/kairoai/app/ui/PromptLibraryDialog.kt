package com.kairoai.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PromptTemplate(
    val title: String,
    val category: String,
    val description: String,
    val prompt: String
)

private val promptTemplates = listOf(
    PromptTemplate("Android ekranı tasarla", "UI/UX", "Bir ekranı durumlarıyla birlikte planla.", "Android için [ekran/özellik] tasarla. Önce kullanıcı amacı ve akışı, ardından hiyerarşi, bileşenler, erişilebilirlik, boş/yükleniyor/hata durumları ve Jetpack Compose uygulama yaklaşımını ver. Bilinmeyenleri soru olarak belirt."),
    PromptTemplate("Compose bileşeni üret", "Android", "Tekrar kullanılabilir, erişilebilir UI bileşeni.", "Jetpack Compose ile [bileşen] oluştur. State'i dışarıdan al, önizlemeler ekle, dark/light temayı ve ekran okuyucu etiketlerini dikkate al. Önce varsayımları ve bağımlılıkları listele, sonra kodu açıkla."),
    PromptTemplate("Hata ayıkla", "Kod", "Hatanın kök nedenini ve güvenli düzeltmeyi bul.", "Şu hatayı analiz et: [hata]. İlgili kod: [kod]. Kök nedeni, doğrulanmış bulguları ve varsayımları ayır. En küçük düzeltmeyi öner; regresyon riski ve test adımlarını da ekle."),
    PromptTemplate("Kod incelemesi", "Kod", "Güvenlik, doğruluk ve bakım açısından incele.", "Aşağıdaki değişikliği deneyimli bir kod inceleyicisi gibi değerlendir: [kod/diff]. Önce yalnızca gerçek sorunları önem sırasına göre yaz; her biri için dosya/satır, etki, neden ve düzeltme önerisi ver. Sorun yoksa açıkça söyle."),
    PromptTemplate("Test planı hazırla", "Kalite", "Kabul kriterlerinden test senaryoları çıkar.", "Şu özellik için test planı hazırla: [özellik]. Normal akış, sınır değerleri, hata/çevrimdışı durum, erişilebilirlik ve regresyonu kapsa. Her senaryoya önkoşul, adımlar ve beklenen sonucu ekle."),
    PromptTemplate("API sözleşmesi tasarla", "Mimari", "İstemci-sunucu sınırlarını ve hataları belirle.", "Şu ihtiyaç için API sözleşmesi tasarla: [ihtiyaç]. Endpoint/HTTP yöntemi, örnek istek-yanıt, kimlik doğrulama, doğrulama, hata şeması, sayfalama ve sürümleme kararlarını belirt. Güvenilmeyen girdileri açıkça ele al."),
    PromptTemplate("Uygulama mimarisi", "Mimari", "Katmanları ve bağımlılıkları sade biçimde çiz.", "Şu uygulama fikri için Android mimarisi öner: [fikir]. Feature/core sınırları, veri akışı, depolama, ağ, hata yönetimi ve test stratejisini açıkla. Gereksiz soyutlama ekleme; küçük başlayıp büyüme yolunu belirt."),
    PromptTemplate("Erişilebilirlik denetimi", "UI/UX", "Kullanılabilirlik ve TalkBack kontrol listesi.", "Bu ekranı erişilebilirlik açısından değerlendir: [ekran/kod]. Dokunma hedefi, kontrast, TalkBack açıklamaları, odak sırası, metin ölçeği, hareket azaltma ve hata mesajlarını kontrol et. Öncelikli düzeltmeleri sırala."),
    PromptTemplate("Performans incelemesi", "Kalite", "Ölçülebilir performans sorunlarını ara.", "Şu Android akışında performans risklerini incele: [kod/akış]. Ana iş parçacığı, gereksiz recomposition, bellek, listeleme, ağ ve görsel yüklemeyi kontrol et. Önce ölçüm yöntemini, sonra kanıta dayalı düzeltmeleri öner."),
    PromptTemplate("Gizlilik kontrolü", "Güvenlik", "Veri akışını ve izinleri tehdit modelle.", "Şu özelliğin gizlilik/güvenlik incelemesini yap: [özellik]. Hangi veri nerede toplanıyor, cihazda mı ağda mı, kim erişiyor, ne kadar saklanıyor? Sırları istemciye gömmeme, izinleri en aza indirme ve kullanıcı onayı konularını kontrol et."),
    PromptTemplate("Release notları", "Ürün", "Kullanıcıya dönük kısa ve anlaşılır notlar yaz.", "Bu değişiklikler için sürüm notu hazırla: [değişiklikler]. Kullanıcı faydasını öne çıkar, teknik jargon kullanma, yeni davranışları ve bilinen sınırlamaları dürüstçe yaz."),
    PromptTemplate("Fikri ürünleştir", "Ürün", "Fikri MVP ve ölçülebilir kabul kriterlerine indir.", "Şu ürün fikrini netleştir: [fikir]. Hedef kullanıcı, temel problem, MVP kapsamı, kapsam dışı kalanlar, riskler ve 3 ölçülebilir kabul kriteri yaz. Önce en kritik belirsizlikleri sor.")
)

@Composable
fun PromptLibraryDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val categories = listOf("Tümü") + promptTemplates.map { it.category }.distinct()
    var selectedCategory by rememberSaveable { mutableStateOf("Tümü") }
    val filtered = remember(selectedCategory) {
        promptTemplates.filter { selectedCategory == "Tümü" || it.category == selectedCategory }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = Panel,
            border = BorderStroke(1.dp, Color(0xFF302B42))
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Violet)
                    Column(Modifier.padding(start = 9.dp)) {
                        Text("Prompt kütüphanesi", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Text("Şablon seç, düzenle ve kendine göre kullan.", color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.title }) { template ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { onSelect(template.prompt) },
                            shape = RoundedCornerShape(17.dp),
                            color = Panel2,
                            border = BorderStroke(1.dp, Color(0xFF242632))
                        ) {
                            Column(Modifier.padding(13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(template.title, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(template.category.uppercase(), color = Mint, fontSize = 8.sp)
                                }
                                Text(template.description, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                Text("Mesaja eklemek için dokun", color = Violet, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Kapat") }
                }
            }
        }
    }
}