package az.uniz.lightcontrol

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var bridge: CabinBridge
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var connStatus: TextView

    // Toggle vəziyyətləri (yalnız UI üçün, əsl vəziyyəti "get" düymələri ilə yoxlamaq olar)
    private var leftOn = false
    private var rightOn = false
    private var fogOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.log)
        scroll = findViewById<View>(R.id.log).parent as ScrollView
        connStatus = findViewById(R.id.connStatus)

        bridge = CabinBridge(applicationContext)
        log(bridge.connect())
        connStatus.text = if (bridge.cabinManager != null) "Qoşuldu ✓" else "Qoşulmadı ✗"

        findViewById<Button>(R.id.btnLeftTurn).setOnClickListener {
            leftOn = !leftOn
            log(bridge.setBoolean("ID_BODY_TURN_LEFT_SIGNAL_STATE", 0, leftOn))
        }
        findViewById<Button>(R.id.btnRightTurn).setOnClickListener {
            rightOn = !rightOn
            log(bridge.setBoolean("ID_BODY_TURN_RIGHT_SIGNAL_STATE", 0, rightOn))
        }
        findViewById<Button>(R.id.btnFog).setOnClickListener {
            fogOn = !fogOn
            log(bridge.setBoolean("ID_FOG_LIGHTS_SWITCH", 0, fogOn))
        }
        findViewById<Button>(R.id.btnHazard).setOnClickListener {
            val on = !leftOn && !rightOn
            leftOn = on; rightOn = on
            log(bridge.setBoolean("ID_BODY_TURN_LEFT_SIGNAL_STATE", 0, on))
            log(bridge.setBoolean("ID_BODY_TURN_RIGHT_SIGNAL_STATE", 0, on))
        }

        // Uzun basanda hazırkı dəyəri oxu (diaqnostika üçün)
        findViewById<Button>(R.id.btnLeftTurn).setOnLongClickListener {
            log(bridge.getBoolean("ID_BODY_TURN_LEFT_SIGNAL_STATE", 0)); true
        }
        findViewById<Button>(R.id.btnRightTurn).setOnLongClickListener {
            log(bridge.getBoolean("ID_BODY_TURN_RIGHT_SIGNAL_STATE", 0)); true
        }
        findViewById<Button>(R.id.btnFog).setOnLongClickListener {
            log(bridge.getBoolean("ID_FOG_LIGHTS_SWITCH", 0)); true
        }
    }

    private fun log(msg: String) {
        logView.append(msg + "\n")
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}
