//package it.giorgini.soundnotes;
//
//import android.content.Context;
//import android.graphics.Typeface;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.TextView;
//
//import java.util.List;
//
//public class ListArrayAdapter<T> extends ArrayAdapter<T> {
//
//    public ListArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
//        super(context, resource, textViewResourceId, objects);
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//
//        View v = super.getView(position, convertView, parent);
//        TextView tv = (TextView) v.findViewById(android.R.id.text1);
//        tv.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/RobotoSlab-Regular.ttf"));
//
//        return v;
//    }
//}

package it.giorgini.soundnotes;

        import android.content.Context;
        import android.graphics.Typeface;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ArrayAdapter;
        import android.widget.BaseAdapter;
        import android.widget.ImageView;
        import android.widget.ListView;
        import android.widget.TextView;

        import java.lang.ref.WeakReference;
        import java.util.List;

public class NoteListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<StorageManager.SoundNote> notes;
    private WeakReference<ViewGroup> listView = new WeakReference<ViewGroup>(null);

    public NoteListAdapter(Context context, List<StorageManager.SoundNote> notes) {
        inflater = LayoutInflater.from(context);
        this.notes = notes;
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public Object getItem(int position) {
        return notes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public ViewGroup getParentView() {
        return listView.get();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StorageManager.SoundNote note = notes.get(position);
        View view;
        ViewHolder holder;
        if (listView.get() == null)
            listView = new WeakReference<>(parent);

        if(convertView == null) {
            view = inflater.inflate(R.layout.note_list_row, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView)view.findViewById(R.id.name);
            holder.text = (TextView)view.findViewById(R.id.text);
            view.setTag(holder);

//            // Aggiungo l'immagine della registrazione se sto registrando (Parte 1)
//            if (RecorderService.getState() == MRState.RECORDING && RecorderService.getCurrRecNoteID().equals(note.id)) {
//                holder.rec = (ImageView) inflater.inflate(R.layout.note_list_rec_image, (ViewGroup) view, false);
//                ((ViewGroup) view).addView(holder.rec);
//            }
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }


        // cambio il font
//        holder.name.setTypeface(Typeface.createFromAsset(parent.getContext().getAssets(), "fonts/RobotoSlab-Regular.ttf"));
//        holder.text.setTypeface(Typeface.createFromAsset(parent.getContext().getAssets(), "fonts/RobotoSlab-Regular.ttf"));
        holder.name.setText(note.name);
        holder.text.setText(note.preview);

        return view;
    }



    private class ViewHolder {
        public TextView name;
        public TextView text;
        public ImageView rec;
    }

}
