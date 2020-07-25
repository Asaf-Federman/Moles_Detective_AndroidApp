package yearly_project.frontend.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import yearly_project.frontend.DB.Information;
import yearly_project.frontend.R;

public class Adapter extends RecyclerView.Adapter<Adapter.MyHolder> {
    private List<Information> informationCollection = new ArrayList<>();
    private OnItemClickListener listener;

    public class MyHolder extends RecyclerView.ViewHolder {
        private TextView date;
        private TextView position;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            this.position = itemView.findViewById(R.id.position);
            this.date = itemView.findViewById(R.id.date);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION)
                    listener.onItemClick(informationCollection.get(position));
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
        Information currentInformation = informationCollection.get(position);
        holder.date.setText(currentInformation.getDate());
        holder.position.setText(String.valueOf(position + 1));
    }

    @Override
    public int getItemCount() {
        return informationCollection.size();
    }

    public void setInformationCollection(List<Information> informationCollection) {
        this.informationCollection = informationCollection;
        notifyDataSetChanged();
    }

    public Information getInformationAt(int pos) {
        return informationCollection.get(pos);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
