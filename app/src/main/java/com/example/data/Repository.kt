package com.example.data

import kotlinx.coroutines.flow.Flow

class SchoolRepository(private val db: AppDatabase) {
    val schoolSetting: Flow<SchoolSetting?> = db.schoolSettingDao.getSettingsFlow()
    val allStudents: Flow<List<Student>> = db.studentDao.getAllStudentsFlow()
    val allMarks: Flow<List<Mark>> = db.markDao.getAllMarksFlow()
    val allAttendance: Flow<List<AttendanceRecord>> = db.attendanceDao.getAllAttendanceFlow()
    val allDiscipline: Flow<List<DisciplineRecord>> = db.disciplineDao.getAllDisciplineFlow()

    suspend fun getSchoolSettingDirect(): SchoolSetting {
        return db.schoolSettingDao.getSettings() ?: SchoolSetting().also {
            db.schoolSettingDao.insertOrUpdate(it)
        }
    }

    suspend fun updateSchoolSetting(setting: SchoolSetting) {
        db.schoolSettingDao.insertOrUpdate(setting)
    }

    fun getStudentsByClassAndSection(className: String, sectionName: String): Flow<List<Student>> {
        return db.studentDao.getStudentsByClassAndSectionFlow(className, sectionName)
    }

    suspend fun getStudentsForSection(className: String, sectionName: String): List<Student> {
        return db.studentDao.getStudentsByClassAndSection(className, sectionName)
    }

    suspend fun getStudentById(id: Int): Student? = db.studentDao.getStudentById(id)

    suspend fun insertStudent(student: Student): Long = db.studentDao.insert(student)

    suspend fun updateStudent(student: Student) = db.studentDao.update(student)

    suspend fun deleteStudent(student: Student) {
        db.studentDao.delete(student)
        db.markDao.deleteMarksForStudent(student.id)
    }

    suspend fun deleteStudentById(studentId: Int) {
        db.studentDao.deleteById(studentId)
        db.markDao.deleteMarksForStudent(studentId)
    }

    fun getMarksForStudentFlow(studentId: Int): Flow<List<Mark>> {
        return db.markDao.getMarksForStudentFlow(studentId)
    }

    suspend fun getMarksForStudent(studentId: Int): List<Mark> {
        return db.markDao.getMarksForStudent(studentId)
    }

    suspend fun saveMark(mark: Mark) {
        db.markDao.insertOrUpdate(mark)
    }

    suspend fun deleteMark(studentId: Int, subjectName: String) {
        db.markDao.deleteMark(studentId, subjectName)
    }

    suspend fun deleteMarkForExam(studentId: Int, subjectName: String, examType: String) {
        db.markDao.deleteMarkForExam(studentId, subjectName, examType)
    }

    // Exam Config operations
    val allExamConfigs: Flow<List<ExamConfig>> = db.examConfigDao.getAllExamConfigsFlow()

    suspend fun getExamConfigForClass(className: String): ExamConfig? {
        return db.examConfigDao.getExamConfigForClass(className)
    }

    suspend fun saveExamConfig(config: ExamConfig) {
        db.examConfigDao.insertOrUpdate(config)
    }

    suspend fun saveExamConfigsBulk(configs: List<ExamConfig>) {
        db.examConfigDao.insertOrUpdateBulk(configs)
    }

    // Section Subject operations
    val allSectionSubjects: Flow<List<SectionSubject>> = db.sectionSubjectDao.getAllSectionSubjectsFlow()

    fun getSubjectsForSectionFlow(className: String, sectionName: String): Flow<List<SectionSubject>> {
        return db.sectionSubjectDao.getSubjectsForSectionFlow(className, sectionName)
    }

    suspend fun getSubjectsForSection(className: String, sectionName: String): List<SectionSubject> {
        return db.sectionSubjectDao.getSubjectsForSection(className, sectionName)
    }

    suspend fun saveSectionSubject(subject: SectionSubject) {
        db.sectionSubjectDao.insertOrUpdate(subject)
    }

    suspend fun saveSectionSubjectsBulk(subjects: List<SectionSubject>) {
        db.sectionSubjectDao.insertOrUpdateBulk(subjects)
    }

    suspend fun deleteSectionSubject(subject: SectionSubject) {
        db.sectionSubjectDao.delete(subject)
    }

    suspend fun deleteSectionSubjectByKeys(className: String, sectionName: String, subjectName: String) {
        db.sectionSubjectDao.deleteByKeys(className, sectionName, subjectName)
    }

    // Attendance operations
    fun getAttendanceForStudentFlow(studentId: Int, termName: String): Flow<List<AttendanceRecord>> {
        return db.attendanceDao.getAttendanceForStudentFlow(studentId, termName)
    }

    suspend fun getAttendanceForStudent(studentId: Int, termName: String): List<AttendanceRecord> {
        return db.attendanceDao.getAttendanceForStudent(studentId, termName)
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord) {
        db.attendanceDao.insertOrUpdate(record)
    }

    suspend fun deleteAttendanceRecord(record: AttendanceRecord) {
        db.attendanceDao.delete(record)
    }

    // Discipline operations
    fun getDisciplineForStudentFlow(studentId: Int, termName: String): Flow<List<DisciplineRecord>> {
        return db.disciplineDao.getDisciplineForStudentFlow(studentId, termName)
    }

    suspend fun getDisciplineForStudent(studentId: Int, termName: String): List<DisciplineRecord> {
        return db.disciplineDao.getDisciplineForStudent(studentId, termName)
    }

    suspend fun saveDisciplineRecord(record: DisciplineRecord) {
        db.disciplineDao.insertOrUpdate(record)
    }

    suspend fun deleteDisciplineRecord(record: DisciplineRecord) {
        db.disciplineDao.delete(record)
    }
}
