package yearly_project.frontend.DB;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;

public class Moles implements Iterable<Mole>{
    private ArrayList<Mole> moles;

    public Moles(){
        moles = new ArrayList<>();
    }

    private ArrayList<Mole> getMoles(){
        return moles;
    }

    public void addMole(Mole mole){
        moles.add(mole);
    }

    public int getSize(){
        return getMoles().size();
    }

    public Mole getMole(int id){
        return getMoles().size()<=id ? null : getMoles().get(id);
    }

    @NonNull
    @Override
    public Iterator<Mole> iterator() {
        return getMoles().iterator();
    }

    public boolean verifyMoles(int maximumAmountOfMoles) throws IllegalAccessException {
        boolean isValid = true;

        for(Mole mole : getMoles()){
            isValid = isValid && mole.isValidMole();
        }

        return isValid && getMoles().size()>0 && getMoles().size() == maximumAmountOfMoles;
    }
}
