import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TestActivity", "onCreate: Starting")
        setContentView(android.R.layout.simple_list_item_1)
        Log.d("TestActivity", "onCreate: Completed")
    }
}