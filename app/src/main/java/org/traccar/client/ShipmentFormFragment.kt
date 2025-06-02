package org.traccar.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import java.util.UUID

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
            val containerId = containerIdInput.text.toString()
            val comment = commentInput.text.toString()

            if (containerId.isNotBlank() && comment.isNotBlank()) {
                val deviceId = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(MainFragment.KEY_DEVICE, null) ?: return@setOnClickListener run {
                    Toast.makeText(requireContext(), "Device ID not found", Toast.LENGTH_SHORT).show()
                }

                val submission = FormSubmission(
                    id = UUID.randomUUID().toString(),
                    containerId = containerId,
                    comment = comment,
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis()
                )

                val db = DatabaseHelper(requireContext())
                db.insertFormSubmissionAsync(submission, object : DatabaseHelper.DatabaseHandler<Unit?> {
                    override fun onComplete(success: Boolean, result: Unit?) {
                        if (success) {
                            Toast.makeText(requireContext(), "Submission saved", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
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