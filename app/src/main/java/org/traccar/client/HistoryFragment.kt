package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class HistoryFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private lateinit var adapter: SubmissionAdapter
    private lateinit var dbHelper: DatabaseHelper
    private val apiService = RetrofitClient.retrofit.create(SyncApiService::class.java)
//
//    data class ShipmentTracking (
//        val id :Int,
//        val deviceId: String,
//    )

//    private  val apiService = object : SyncApiService {
//    override suspend fun sendPosition(position: Position): Unit = Unit
//    override suspend fun sendFormData(submission: FormSubmission): Unit = Unit
//    override suspend fun login(request: LoginRequest): LoginResponse = throw NotImplementedError()
//    override suspend fun verifyCode(request: CodeVerificationRequest): CodeVerificationResponse = throw NotImplementedError()
//    override suspend fun getShipmentHistory(userId: String): List<ShipmentTracking> {
//        return listOf(
//            ShipmentTracking(
//                id = 1,
//                deviceId = "hd2334s",
////                userId = userId.toLong()
//            )
//        ).takeLast(20) // Mimic the .takeLast(20) in the original code
//    }
//}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext()) // Initialize after onAttach
        recyclerView = view.findViewById(R.id.submission_list)

        if (recyclerView != null) {
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
            adapter = SubmissionAdapter()
            recyclerView?.adapter = adapter
            fetchSubmissions()
        } else {
            Log.e("History", "RecyclerView is null")
        }
    }

    private fun fetchSubmissions() {
        lifecycleScope.launch {
            try {
                val user = dbHelper.selectUser() ?: run {
                    Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                    return@launch
                }

                val submissions = withContext(Dispatchers.IO) {
                    apiService.getShipmentHistory(user.id.toString()).takeLast(20)
                }
                Log.d("History", "Submissions: ${submissions}")
                adapter.setSubmissions(submissions)
                Log.d("History", "Submissions set")
                if (submissions.isEmpty()) {
                    Log.e("History", "No submissions found")
                }else {
                    // Save the first shipmentTrackingId to SharedPreferences
                    val firstShipmentTrackingId = submissions.firstOrNull()?.id ?: -1
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putInt("first_shipment_tracking_id", firstShipmentTrackingId)
                        .apply()
                    Log.d("History", "Saved first shipmentTrackingId: $firstShipmentTrackingId")
                }
            } catch (e: HttpException) {
                if (e.code() == 401) { // Fixed deprecated code()
                    Log.e("History", "Session expired. Please log in again.")

                    if (dbHelper.clearUserDataSync()) {
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Log.e("History", "Failed to clear user data")
//                        Toast.makeText(requireContext(), "Failed to clear user data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class SubmissionAdapter : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

        private var submissions: List<ShipmentTracking> = emptyList()

        fun setSubmissions(submissions: List<ShipmentTracking>) {
            this.submissions = submissions
            Log.d("History","Adapter Submissions ${this.submissions}, ${submissions}")
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
            return SubmissionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
            val submission = submissions[position]
            holder.containerId.text = submission.id.toString()
            holder.comment.text = submission.deviceId

            Log.d("History", "On  Bind ViewHolder: ${holder.containerId.text}, ${holder.comment.text}, ${submission}")
        }

        override fun getItemCount(): Int = submissions.size

        inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val containerId: TextView = itemView.findViewById(R.id.container_id)
            val comment: TextView = itemView.findViewById(R.id.comment)
        }
    }
}