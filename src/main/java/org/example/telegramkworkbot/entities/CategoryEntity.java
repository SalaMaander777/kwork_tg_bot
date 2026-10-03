package org.example.telegramkworkbot.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "categories")
public class CategoryEntity
{
    @Id
    @Column(name = "kwork_category_id")
    private long id;

    @Column(name = "category_name")
    private String name;
}
