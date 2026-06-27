package com.myschedule.id

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myschedule.id.ui.*
import com.myschedule.id.ui.theme.MyScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyScheduleTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("signup") { SignUpScreen(navController) }
                    composable("reset") { ResetPasswordScreen(navController) }
                    composable("teacher_home") { TeacherHomeScreen(navController) }
                    composable("profiledosen") { ProfileDosenScreen(navController) }
                    composable("teacher_manage_campus") { TeacherManageCampusScreen(navController) }
                    composable("student_home") { StudentHomeScreen(navController) }
                    composable("student_profile") { ProfileScreen(navController) }
                    composable("student_tasks_overview") { StudentTasksOverviewScreen(navController) }
                    composable("student_schedule_overview") { StudentScheduleOverviewScreen(navController) }

                    composable(
                        route = "teacher_manage_uni/{uniName}",
                        arguments = listOf(navArgument("uniName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        TeacherManageUniScreen(navController, uniName)
                    }

                    composable(
                        route = "teacher_class_dashboard/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherClassDashboardScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_manage_absensi/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherAbsensiScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_absensi_detail/{absensiId}/{title}",
                        arguments = listOf(
                            navArgument("absensiId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val absensiId = backStackEntry.arguments?.getString("absensiId") ?: ""
                        val title = backStackEntry.arguments?.getString("title") ?: ""
                        TeacherAbsensiDetailScreen(navController, absensiId, title)
                    }

                    composable(
                        route = "teacher_manage_tasks/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherManageTasksScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_manage_quiz/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherManageQuizScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_create_quiz/{uniName}/{className}?quizId={quizId}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType },
                            navArgument("quizId") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                        TeacherCreateQuizScreen(navController, uniName, className, quizId)
                    }

                    composable(
                        route = "teacher_quiz_results/{quizId}/{quizTitle}",
                        arguments = listOf(
                            navArgument("quizId") { type = NavType.StringType },
                            navArgument("quizTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                        val quizTitle = backStackEntry.arguments?.getString("quizTitle") ?: ""
                        TeacherQuizResultsScreen(navController, quizId, quizTitle)
                    }

                    composable(
                        route = "teacher_manage_materials/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherManageMaterialsScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_task_submissions/{taskId}/{taskTitle}",
                        arguments = listOf(
                            navArgument("taskId") { type = NavType.StringType },
                            navArgument("taskTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                        val taskTitle = backStackEntry.arguments?.getString("taskTitle") ?: ""
                        TeacherTaskSubmissionsScreen(navController, taskId, taskTitle)
                    }

                    composable(
                        route = "teacher_student_tasks_grades/{studentUid}/{studentName}/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("studentUid") { type = NavType.StringType },
                            navArgument("studentName") { type = NavType.StringType },
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val studentUid = backStackEntry.arguments?.getString("studentUid") ?: ""
                        val studentName = backStackEntry.arguments?.getString("studentName") ?: ""
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherStudentTasksGradesScreen(navController, studentUid, studentName, uniName, className)
                    }

                    composable(
                        route = "student_class_dashboard/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentClassDashboardScreen(navController, uniName, className)
                    }

                    composable(
                        route = "student_tasks/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentTugasScreen(navController, uniName, className)
                    }

                    composable(
                        route = "student_quiz/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentQuizScreen(navController, uniName, className)
                    }

                    composable(
                        route = "student_quiz_attempt/{quizId}/{quizTitle}",
                        arguments = listOf(
                            navArgument("quizId") { type = NavType.StringType },
                            navArgument("quizTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                        val quizTitle = backStackEntry.arguments?.getString("quizTitle") ?: ""
                        StudentQuizAttemptScreen(navController, quizId, quizTitle)
                    }

                    composable(
                        route = "student_materi/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentMaterialsScreen(navController, uniName, className)
                    }

                    composable(
                        route = "student_grades/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentGradesScreen(navController, uniName, className)
                    }

                    composable(
                        route = "student_absensi/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        StudentAbsensiScreen(navController, uniName, className)
                    }

                    composable("logout") { LogoutScreen(navController) }
                    composable(
                        route = "teacher_class_students/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherClassStudentsScreen(navController, uniName, className)
                    }

                    composable(
                        route = "teacher_class_grades/{uniName}/{className}",
                        arguments = listOf(
                            navArgument("uniName") { type = NavType.StringType },
                            navArgument("className") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val uniName = backStackEntry.arguments?.getString("uniName") ?: ""
                        val className = backStackEntry.arguments?.getString("className") ?: ""
                        TeacherClassGradesScreen(navController, uniName, className)
                    }
                    composable(
                        route = "file_viewer?fileUrl={fileUrl}&fileName={fileName}",
                        arguments = listOf(
                            navArgument("fileUrl") { type = NavType.StringType; defaultValue = "" },
                            navArgument("fileName") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val fileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                        val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                        FileViewerScreen(navController, fileUrl, fileName)
                    }
                }
            }
        }
    }
    private fun openKostSpaceee() {

        val intent = packageManager.getLaunchIntentForPackage(
            "com.company.kostspaceee"
        )

        if (intent != null) {

            startActivity(intent)

        } else {

            Toast.makeText(
                this,
                "APK KostSpaceee belum terinstall",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
