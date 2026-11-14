# UNIZ Light Control

Android Automotive-un öz `android.car.hardware.cabin.CarCabinManager` API-sına
reflection ilə qoşulub sol/sağ dönmə işığını və duman işığını əl ilə idarə edən
müstəqil tətbiq. FactoryApp-a, Settings-ə bağlı deyil.

## İcazə məsələsi
`android.car.permission.CAR_VENDOR_EXTENSION` çox güman ki, "signature|privileged"
səviyyəlidir. Adi quraşdırma ilə işləməsə:
1. Root olduğun üçün, APK-nı Magisk modulu kimi `/system/priv-app/UnizLightControl/`
   altına qoy (eyni adda qovluq + APK).
2. Yenidən başlat. Bu, tətbiqi "privileged" sistem tətbiqi kimi tanıdır və
   permission grant olma ehtimalını artırır.

## Sınaq
Hər düymənin jurnalında (log) nəticə görünür: "OK: ..." uğurlu çağırışdır,
"XƏTA: ..." isə hansı addımda problem olduğunu göstərir (ClassNotFoundException,
SecurityException, NoSuchFieldException və s.). Bu mətni mənə göndər ki,
növbəti addımı dəqiqləşdirək.

Düymələrə **uzun basmaqla** hazırkı vəziyyəti oxumaq (get) mümkündür.
