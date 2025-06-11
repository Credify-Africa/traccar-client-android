package org.traccar.client

//class SubmissionAdapter(private val submissions: List<FormSubmission>) :
//    RecyclerView.Adapter<SubmissionAdapter.ViewHolder>() {
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val containerId: TextView = itemView.findViewById(R.id.container_id)
//        val comment: TextView = itemView.findViewById(R.id.comment)
////        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_submission, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val submission = submissions[position]
//        holder.containerId.text = "Container ID: ${submission.id}"
//        holder.comment.text = submission.
////        holder.timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
////            .format(Date(submission.timestamp))
//    }
//
//    override fun getItemCount(): Int = submissions.size
//}