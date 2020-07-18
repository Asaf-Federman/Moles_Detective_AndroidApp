package yearly_project.frontend.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;

public class Adapter extends RecyclerView.Adapter<MyHolder> {

    private final Context context;

    public Adapter(Context context){
        this.context = context;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout,null));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.getDate().setText(UserInformation.getInformation(position).getDate().toString());
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
