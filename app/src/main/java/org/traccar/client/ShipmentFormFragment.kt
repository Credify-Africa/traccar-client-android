package org.traccar.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import java.util.UUID
import androidx.work.WorkManager

class ShipmentFormFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.shipment_submission_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val containerIdInput = view.findViewById<EditText>(R.id.container_id)
        val commentInput = view.findViewById<EditText>(R.id.comment)
        val submitButton = view.findViewById<Button>(R.id.submit_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        submitButton.setOnClickListener {
//            val containerId = containerIdInput.text.toString()
            val comment = commentInput.text.toString()

            if (comment.isNotBlank()) {
                val deviceId = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(MainFragment.KEY_DEVICE, null) ?: return@setOnClickListener run {
                    Toast.makeText(requireContext(), "Device ID not found", Toast.LENGTH_SHORT).show()
                }

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val shipmentTrackingId = sharedPreferences.getInt("first_shipment_tracking_id", -1).toString()
                if (shipmentTrackingId == "-1") {
                    Toast.makeText(requireContext(), "Shipment Tracking ID not found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val submission = FormSubmission(
                    id = UUID.randomUUID().toString(),
                    eventDesciption = comment,
                    eventTimeStamp = System.currentTimeMillis(),
                    shipmentTrackingId = shipmentTrackingId
                )

                val db = DatabaseHelper(requireContext())
                db.insertFormSubmissionAsync(submission, object : DatabaseHelper.DatabaseHandler<Unit?> {
                    override fun onComplete(success: Boolean, result: Unit?) {
                        if (success) {
                            Toast.makeText(requireContext(), "Submission saved", Toast.LENGTH_SHORT).show()
                            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                                .build()
                            WorkManager.getInstance(requireContext())
                                .enqueue(syncWorkRequest)
                            findNavController().navigate(R.id.historyFragment)
                        } else {
                            Log.e("ShipmentForm", "Failed To save Submission")
                            Toast.makeText(requireContext(), "Failed to save submission", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}