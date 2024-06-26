package com.example.primeraentrega;

public class Recipe {
    // atributos
    private int id;
    private String nombre;
    private String ingredientes;
    private String descripcion;
    private boolean vegano;
    private int idImagen;

    // constructora
    public Recipe(int id, String nombre, String ingredientes, String descripcion, boolean vegano, int idImagen) {
        this.id = id;
        this.nombre = nombre;
        this.ingredientes = ingredientes;
        this.descripcion = descripcion;
        this.vegano = vegano;
        this.idImagen = idImagen;
    }

    // métodos getter y setter para los atributos
    public int getId() { return id; }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(String ingredientes) {
        this.ingredientes = ingredientes;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean getVegano() {
        return vegano;
    }

    public void setVegano(Boolean vegano) {
        this.vegano = vegano;
    }

    public void setIdImagen(int idImagen) {
        this.idImagen = idImagen;
    }

    public int getIdImagen() {
        return this.idImagen;
    }
}
