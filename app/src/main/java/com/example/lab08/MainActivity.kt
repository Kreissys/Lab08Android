package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.lab08.data.TaskDatabase
import com.example.lab08.viewmodel.TaskViewModel
import com.example.lab08.ui.theme.Lab08Theme
import kotlinx.coroutines.launch
import com.example.lab08.model.Task

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                // ✅ Configuración de Room con migración y acceso en hilo principal
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // ⚠️ Solo para desarrollo
                    .build()

                val viewModel = TaskViewModel(db.taskDao())
                TaskScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var newTaskDescription by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var editedDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tareas", style = MaterialTheme.typography.titleLarge) }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // 🔹 Campo de texto para nueva tarea
                TextField(
                    value = newTaskDescription,
                    onValueChange = { newTaskDescription = it },
                    label = { Text("Nueva tarea") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (newTaskDescription.isNotEmpty()) {
                            viewModel.addTask(newTaskDescription)
                            newTaskDescription = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Agregar tarea")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Lista de tareas
                tasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = task.description,
                                    style = if (task.isCompleted)
                                        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
                                    else
                                        MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Creada: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", task.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row {
                                IconButton(onClick = {
                                    taskToEdit = task
                                    editedDescription = task.description
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                }

                                IconButton(onClick = {
                                    coroutineScope.launch { viewModel.deleteTask(task) }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                }

                                Button(onClick = { viewModel.toggleTaskCompletion(task) }) {
                                    Text(if (task.isCompleted) "✔" else "⏳")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Botón para eliminar todas las tareas
                Button(
                    onClick = { coroutineScope.launch { viewModel.deleteAllTasks() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Eliminar todas las tareas")
                }
            }

            // 🔹 Diálogo para editar tarea
            if (showEditDialog && taskToEdit != null) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Editar tarea") },
                    text = {
                        TextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                            label = { Text("Nueva descripción") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            taskToEdit?.let { viewModel.updateTaskDescription(it, editedDescription) }
                            showEditDialog = false
                        }) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    )
}
