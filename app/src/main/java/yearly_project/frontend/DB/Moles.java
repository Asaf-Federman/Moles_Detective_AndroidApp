package yearly_project.frontend.DB;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Moles implements Iterable<Mole>{
    private Collection<Mole> moles;

    public Moles(){
        moles = new ArrayList<>();
    }

    private Collection<Mole> getMoles(){
        return moles;
    }

    public void addMole(Mole mole){
        moles.add(mole);
    }

    public int getSize(){
        return getMoles().size();
    }

    @NonNull
    @Override
    public Iterator<Mole> iterator() {
        return getMoles().iterator();
    }
}
