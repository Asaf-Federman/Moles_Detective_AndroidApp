package yearly_project.frontend.home;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import yearly_project.frontend.R;

public class MyHolder extends ViewHolder {
    private TextView date;

    public MyHolder(@NonNull View itemView) {
        super(itemView);

        this.date = itemView.findViewById(R.id.date);
    }

    public TextView getDate() {
        return date;
    }

    public void setDate(TextView date) {
        this.date = date;
    }
}
