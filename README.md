# PaintPack

Minecraft **Fabric** (1.21 ve uzeri) icin, elinizde tuttugunuz esyanin
texture'ini oyun icinde gercek zamanli olarak boyamanizi saglayan bir mod.

Yalnizca Fabric destekler. Forge, NeoForge ve Quilt icin hicbir kod
bulunmaz.

## Ozellikler

- Ayarlanabilir kisayol tusu (varsayilan **P**) ile Esya Boyama Editorunu ac
- Renk secici: RGB, HSV (doygunluk/parlaklik kutusu + ton seridi), HEX girisi
- Son kullanilan renkler listesi
- Firca ve silgi (mouse tekerlegi ile firca boyutu ayari)
- Piksel izgarasi ve buyutulmus, akici canli onizleme
- Sinirsiz benzeri Geri Al / Ileri Al (CTRL+Z / CTRL+Y)
- Kaydedilen texture'lar `.minecraft/config/paintpack/textures/` klasorunde
  PNG olarak saklanir; Minecraft yeniden baslatildiginda otomatik yuklenir
- **Orijinal Minecraft dosyalarina veya modellerine kesinlikle dokunulmaz**

## Onemli tasarim notu

Bir esya boyandiginda, boyama o **esya turune** (orn. Elmas Kilic) uygulanir;
yalnizca o an elinizdeki tekil esya kopyasina degil. Bunun sebebi
Minecraft'ta bir esya turunun normalde tek bir paylasilan texture'i
olmasidir. Bu sayede sistem, hicbir orijinal dosyayi degistirmeden,
tamamen bellek icinde (RAM/GPU) calisir.

Boyanan gorunum su an icin oncelikli olarak **elde tutulan ve yerde duran
esya** render'inda uygulanir (duz/flat bir kare olarak cizilir). Envanter
ekranindaki 2 boyutlu ikonun da tam olarak degistirilmesi, Minecraft'in
texture atlas sistemine daha derin entegrasyon gerektirir; bu, projenin
bir sonraki gelistirme adimi olarak `PaintedTextureManager` sinifinda
belirtilmistir.

## Surum uyumlulugu

Mod, `1.21.1` mappingleri ile derlenir ve `fabric.mod.json` icinde
`"minecraft": ">=1.21"` olarak tanimlanir; yani **1.21 ve sonrasindaki
tum surumlerle uyumlu** olarak isaretlenir (ust sinir yoktur). Bu mod
yalnizca uzun sureli, kararli Fabric/Minecraft API'lerini kullandigi
icin 1.21.x hattinin farkli yama surumlerinde de calismasi beklenir.

Onemli: Tek bir jar dosyasiyla her 1.21.x surumunu ayri ayri test edip
garanti etmek (orn. Stonecutter/Architectury gibi coklu-surum derleme
sistemleri olmadan) mumkun degildir. Ileride bir surum, kullanilan bir
API'yi kaldirir/degistirirse, o surume ozel kucuk bir guncelleme
gerekebilir.

## Gradle Wrapper hakkinda

`gradle-wrapper.jar` bir binary (ikili) dosyadir ve bu repoya elle
eklenmemistir. `.github/workflows/build.yml` icindeki CI is akisi, derleme
baslamadan once Gradle'i kurup `gradle wrapper --gradle-version 8.8`
komutuyla bu dosyayi otomatik olarak uretir. Bu sayede depoyu GitHub'a
yukledikten hemen sonra GitHub Actions sorunsuz sekilde derleme yapar.

Yerel bilgisayarinizda derlemek isterseniz, once bir kez
`gradle wrapper --gradle-version 8.8` komutunu calistirmaniz gerekir
(sisteminizde Gradle kurulu olmasi sarttir), ardindan `./gradlew build`
normal sekilde calisir.

## Derleme

```
./gradlew build
```

Derlenen mod dosyasi `build/libs/paintpack-1.0.0.jar` altinda olusur.

## Uyumluluk

- Vanilla
- Resource Pack
- Sodium
- Iris
- Mod Menu

## Lisans

MIT - bkz. [LICENSE](LICENSE)
