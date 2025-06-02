package org.traccar.client

import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HistoryFragment : Fragment() {

    private  var recyclerView: RecyclerView? = null
    private lateinit var adapter: SubmissionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubmissionAdapter()
        recyclerView?.adapter = adapter

        fetchSubmissions()
    }

    private fun fetchSubmissions() {
        lifecycleScope.launch {
            try {
                // Retrieve user ID from shared preferences
                val userId = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("KEY_USER_ID", null)
                if (userId == null) {
                    Toast.makeText(requireContext(), "User ID not found. Please log in.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val submissions: List<FormSubmission> = withContext(Dispatchers.IO) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://your-server.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val service = retrofit.create(SyncApiService::class.java)
                    service.getShipmentHistory(userId).takeLast(20)
                }
                adapter.setSubmissions(submissions)
                if (submissions.isEmpty()) {
                    Toast.makeText(requireContext(), "No submissions found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


class SubmissionAdapter : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    private var submissions: List<FormSubmission> = emptyList()

    fun setSubmissions(submissions: List<FormSubmission>) {
        this.submissions = submissions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val submission = submissions[position]
        holder.containerId.text = submission.containerId
        holder.comment.text = submission.comment
//        holder.timestamp.text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(submission.timestamp))
    }

    override fun getItemCount(): Int = submissions.size

    class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerId: TextView = itemView.findViewById(R.id.container_id)
        val comment: TextView = itemView.findViewById(R.id.comment)
//        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
    }
}