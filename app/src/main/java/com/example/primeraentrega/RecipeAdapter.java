package com.example.primeraentrega;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.primeraentrega.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    // lista actualizada de las recetas
    private static List<Recipe> recipeList;
    private String URL_BAJADA = "http://34.65.250.38:8080/bajar_imagen.php";

    public RecipeAdapter(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }
    private OnDeleteClickListener deleteClickListener;
    private OnRecipeClickListener onRecipeClickListener;


    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, final int position) {
        Recipe recipe = recipeList.get(position);

        // configurar los elementos de la receta en el CardView
        holder.recipeNameTextView.setText(recipe.getNombre());

        // verificar si el ID de la imagen es 0
        if (recipe.getIdImagen() == 0) {
            // utilizar la imagen predeterminada
            holder.recipeImageView.setImageResource(R.drawable.default_recipe_image_foreground);
        }
        else {
            bajarImagen(recipe.getIdImagen(), holder);
        }

        // si la receta es vegana, añadirle la etiqueta de "vegano"
        if (recipe.getVegano()) {
            holder.vegano.setText(R.string.vegano_label);
        } else {
            holder.vegano.setText("");
        }

        // preparar botón de eliminar receta
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    if (deleteClickListener != null) {
                        deleteClickListener.onDeleteClick(adapterPosition);
                    }
                }
            }
        });

        // preparar listener para la receta
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    if (onRecipeClickListener != null) {
                        onRecipeClickListener.onRecipeClick(adapterPosition);
                    }
                }
            }
        });
    }

    // obtener cantidad de recetas en la lista
    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    // listener personalizado para manejar el evento de eliminar receta
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    // listener personalizado para manejar clics en elementos de Recetas
    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.onRecipeClickListener = listener;
    }

    // viewHolder para las vistas de elementos individuales en el RecyclerView
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        public ImageView recipeImageView;
        private TextView recipeNameTextView;
        private TextView vegano;
        private Button deleteButton;

        // constructora de elementos individuales
        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImageView = itemView.findViewById(R.id.recipeImageView);
            recipeNameTextView = itemView.findViewById(R.id.recipeNameTextView);
            vegano = itemView.findViewById(R.id.vegano);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnDeleteClickListener {
        // la posición del elemento del RecyclerView
        void onDeleteClick(int position);
    }

    public interface OnRecipeClickListener {
        // la posición del elemento del RecyclerView
        void onRecipeClick(int position);
    }

    // descargar imagen del servidor
    private void bajarImagen(int idImagen, RecipeViewHolder holder) {
        // preparar datos
        Data inputData = new Data.Builder()
                .putInt("id_imagen", idImagen)
                .putString("key", "bajar_imagen")
                .putString("url", URL_BAJADA)
                .build();

        OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ImageManager.class)
                .setInputData(inputData)
                .build();

        Context context = holder.itemView.getContext();

        WorkManager.getInstance(context).enqueue(registerRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(registerRequest.getId()).observe((LifecycleOwner) context, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    String imagenBase64 = workInfo.getOutputData().getString("imagen");
                    if (success && !imagenBase64.isEmpty()) {
                        // imagen bajada correctamente
                        byte[] decodedBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                        Bitmap imagen = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        holder.recipeImageView.setImageBitmap(imagen);
                    } else {
                        // fallo al bajar la imagen
                        Drawable drawable = context.getResources().getDrawable(R.drawable.default_recipe_image_foreground);
                        // obtener la referencia a la ImageView desde el ViewHolder
                        holder.recipeImageView.setImageDrawable(drawable);
                    }
                }
            }
        });
    }
}
