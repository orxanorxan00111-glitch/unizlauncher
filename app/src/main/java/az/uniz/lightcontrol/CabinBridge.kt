package az.uniz.lightcontrol

import android.content.Context
import java.lang.reflect.Method

/**
 * android.car.hardware.cabin.CarCabinManager-ə reflection ilə qoşulma.
 * Bu sinif compile zamanı android.car SDK-sına ehtiyac duymur, çünki
 * bütün siniflər və sabitlər cihazın öz runtime-ından oxunur.
 */
class CabinBridge(private val context: Context) {

    var car: Any? = null
        private set
    var cabinManager: Any? = null
        private set

    /** Car servisinə qoşulur. Nəticəni mətn kimi qaytarır (uğur və ya xəta izahı). */
    fun connect(): String {
        return try {
            val carClass = Class.forName("android.car.Car")

            // createCar(Context) - köhnə, sinxron versiya (Android 9-da mövcuddur)
            val createCar: Method = carClass.getMethod("createCar", Context::class.java)
            val carObj = createCar.invoke(null, context)
                ?: return "XƏTA: Car.createCar() null qaytardı (servis mövcud deyil?)"
            car = carObj

            val cabinServiceNameField = carClass.getField("CABIN_SERVICE")
            val cabinServiceName = cabinServiceNameField.get(null) as String

            val getCarManager: Method = carClass.getMethod("getCarManager", String::class.java)
            val cm = getCarManager.invoke(carObj, cabinServiceName)
                ?: return "XƏTA: CarCabinManager alına bilmədi (icazə yoxdur?)"
            cabinManager = cm
            "OK: Car servisinə qoşuldu, CarCabinManager alındı."
        } catch (e: Throwable) {
            "XƏTA (connect): ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    /** Verilən ID adına (məsələn "ID_FOG_LIGHTS_SWITCH") uyğun boolean property-ni dəyişir. */
    fun setBoolean(idFieldName: String, area: Int, value: Boolean): String {
        val cm = cabinManager ?: return "XƏTA: əvvəlcə connect() uğurlu olmalıdır."
        return try {
            val cabinManagerClass = Class.forName("android.car.hardware.cabin.CarCabinManager")
            val idField = cabinManagerClass.getField(idFieldName)
            val id = idField.get(null) as Int

            val setMethod: Method = cabinManagerClass.getMethod(
                "setBooleanProperty",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
            )
            setMethod.invoke(cm, id, area, value)
            "OK: $idFieldName = $value (area=$area)"
        } catch (e: NoSuchFieldException) {
            "XƏTA: $idFieldName adlı sabit tapılmadı (bu versiyada adı fərqli ola bilər)."
        } catch (e: Throwable) {
            "XƏTA ($idFieldName): ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    /** Eyni property-nin hazırkı dəyərini oxuyur (sınaq/diaqnostika üçün). */
    fun getBoolean(idFieldName: String, area: Int): String {
        val cm = cabinManager ?: return "XƏTA: əvvəlcə connect() uğurlu olmalıdır."
        return try {
            val cabinManagerClass = Class.forName("android.car.hardware.cabin.CarCabinManager")
            val idField = cabinManagerClass.getField(idFieldName)
            val id = idField.get(null) as Int

            val getMethod: Method = cabinManagerClass.getMethod(
                "getBooleanProperty",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            val result = getMethod.invoke(cm, id, area)
            "OK: $idFieldName = $result"
        } catch (e: Throwable) {
            "XƏTA ($idFieldName oxuma): ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
