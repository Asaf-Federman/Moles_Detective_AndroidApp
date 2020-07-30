package yearly_project.frontend.home;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import yearly_project.frontend.DB.Information;
import yearly_project.frontend.R;

public class Adapter extends ListAdapter<Information,Adapter.MyHolder> {
    private OnItemClickListener listener;
    private static final DiffUtil.ItemCallback<Information> DIFF_CALLBACK = new DiffUtil.ItemCallback<Information>() {
        @Override
        public boolean areItemsTheSame(@NonNull Information oldItem, @NonNull Information newItem) {
            return oldItem.getSerialNumber() == newItem.getSerialNumber();
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Information oldItem, @NonNull Information newItem) {
            return oldItem.getJson().equals(newItem.getJson());
        }
    };

    protected Adapter() {
        super(DIFF_CALLBACK);
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private TextView date;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            this.date = itemView.findViewById(R.id.date);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION)
                    listener.onItemClick(getItem(position));
            });
        }
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Information currentInformation = getItem(position);
        holder.date.setText(currentInformation.getDate());
    }

    public Information getInformationAt(int pos) {
        return getItem(pos);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
