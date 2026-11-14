package az.uniz.lightcontrol

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * android.car.hardware.cabin.CarCabinManager-ə reflection ilə qoşulma.
 * Car.createCar() metodunun imzası Android versiyasından asılı olaraq
 * dəyişdiyi üçün, bu sinif cihazda mövcud olan BÜTÜN overload-ları
 * avtomatik tapıb sınayır (tək Context, Context+Handler, lifecycle-listener və s.).
 */
class CabinBridge(private val context: Context) {

    var car: Any? = null
        private set
    var cabinManager: Any? = null
        private set

    fun connect(): String {
        val log = StringBuilder()
        try {
            val carClass = Class.forName("android.car.Car")
            val methods = carClass.methods.filter {
                it.name == "createCar" && Modifier.isStatic(it.modifiers)
            }
            log.append("Tapılan createCar overload sayı: ${methods.size}\n")
            for (m in methods) {
                log.append("  • ${m.parameterTypes.joinToString(", ") { t -> t.simpleName }}\n")
            }

            var resultCar: Any? = null
            var usedSignature = ""
            for (m in methods.sortedBy { it.parameterCount }) {
                resultCar = tryInvokeCreateCar(carClass, m, log)
                if (resultCar != null) {
                    usedSignature = m.parameterTypes.joinToString(",") { t -> t.simpleName }
                    break
                }
            }

            if (resultCar == null) {
                return log.append("XƏTA: heç bir createCar overload-u Car qaytarmadı.").toString()
            }
            log.append("UĞUR: createCar($usedSignature) ilə Car alındı.\n")
            car = resultCar

            val cabinServiceName = carClass.getField("CABIN_SERVICE").get(null) as String
            val getCarManager: Method = carClass.getMethod("getCarManager", String::class.java)
            val cm = getCarManager.invoke(resultCar, cabinServiceName)
            if (cm == null) {
                return log.append("XƏTA: CarCabinManager null qayıtdı (icazə yoxdur?)").toString()
            }
            cabinManager = cm
            return log.append("OK: CarCabinManager alındı.").toString()
        } catch (e: Throwable) {
            return log.append("XƏTA (connect ümumi): ${e.javaClass.simpleName}: ${e.message}").toString()
        }
    }

    /** Bir createCar overload-unu parametr tiplərinə uyğun arqumentlərlə çağırmağa çalışır. */
    private fun tryInvokeCreateCar(carClass: Class<*>, m: Method, log: StringBuilder): Any? {
        return try {
            val paramTypes = m.parameterTypes
            val args = arrayOfNulls<Any?>(paramTypes.size)
            var latch: CountDownLatch? = null
            val resultHolder = arrayOfNulls<Any?>(1)

            for (i in paramTypes.indices) {
                val pt = paramTypes[i]
                args[i] = when {
                    pt == Context::class.java -> context
                    pt == Handler::class.java -> Handler(Looper.getMainLooper())
                    pt == Looper::class.java -> Looper.getMainLooper()
                    pt == Long::class.javaPrimitiveType -> 8000L
                    pt.isInterface -> {
                        // Köhnə ServiceConnection və ya yeni CarServiceLifecycleListener kimi
                        // callback interfeyslərini dinamik proxy ilə saxtalaşdırırıq.
                        latch = CountDownLatch(1)
                        Proxy.newProxyInstance(pt.classLoader, arrayOf(pt)) { _, method, methodArgs ->
                            val carArg = methodArgs?.firstOrNull {
                                it != null && it.javaClass.name == "android.car.Car"
                            }
                            if (carArg != null) resultHolder[0] = carArg
                            latch?.countDown()
                            if (method.returnType == Boolean::class.javaPrimitiveType) false else null
                        }
                    }
                    else -> null
                }
            }

            val invokeResult = m.invoke(null, *args)
            when {
                invokeResult != null && invokeResult.javaClass.name == "android.car.Car" -> invokeResult
                latch != null -> {
                    latch.await(6, TimeUnit.SECONDS)
                    resultHolder[0]
                }
                else -> null
            }
        } catch (e: Throwable) {
            val cause = e.cause ?: e
            log.append("    uğursuz (${m.parameterTypes.size} parametr): ${cause.javaClass.simpleName}: ${cause.message}\n")
            null
        }
    }

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
            val cause = e.cause ?: e
            "XƏTA ($idFieldName): ${cause.javaClass.simpleName}: ${cause.message}"
        }
    }

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
            val cause = e.cause ?: e
            "XƏTA ($idFieldName oxuma): ${cause.javaClass.simpleName}: ${cause.message}"
        }
    }
}
