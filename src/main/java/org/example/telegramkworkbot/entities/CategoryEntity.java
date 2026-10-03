package org.example.telegramkworkbot.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(name = "categories")
public class CategoryEntity
{
    @Id
    @Column(name = "kwork_category_id")
    private long category_id;

    @Column(name = "category_name")
    private String category_name;
}
