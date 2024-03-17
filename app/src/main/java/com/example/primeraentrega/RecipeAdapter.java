package com.example.primeraentrega;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.primeraentrega.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    // lista actualizada de las recetas
    private static List<Recipe> recipeList;

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
        private TextView recipeNameTextView;
        private TextView vegano;
        private Button deleteButton;

        // constructora de elementos individuales
        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
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
}
