package ta

import org.apache.ivy.core.settings.Validatable

import java.text.SimpleDateFormat
import java.lang.*
import ta.Evaluation

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@SuppressWarnings("GroovyMissingReturnStatement")
@Transactional(readOnly = true)
class StudentController {

	static allowedMethods = [update: "PUT"]

	//formattedDate foi removido pois ele estava no EvaluationController

	def index(Integer max) {
		params.max = Math.min(max ?: 100, 100)
		respond Student.list(params), model: [studentInstanceCount: Student.count()]
	}

	public double checkPercentageEvaluationStudent(String evalValue, String loginA) {
		def student = Student.findByLogin(loginA)
		int contE = 0
		def evaluationLists = student.criteriaAndEvaluations;
		int tamanho = 0;
		for (int i = 0; i < evaluationLists.size(); i++) {
			def evaluat = evaluationLists[i].getEvaluations()
			tamanho += evaluat.size()
			for (int j = 0; j < evaluat.size(); j++) {
				if (evaluat.get(i).value.equalsIgnoreCase(evalValue)) {
					contE++
				}
			}
		}
		return contE / tamanho
	}

	def updateAllAverages() {
		Student.list().each {
			List<EvaluationsByCriterion> cae = it.criteriaAndEvaluations
			for (int i = 0; i < cae.size(); i++) {
				cae[i].doMedia()
			}
			it.calcMedia()
		}
	}

	def List evaluate(String eval, int qtdEvaluations, double tempMedia) {
		double resultMedia = tempMedia
		int resultQtdEvaluations = qtdEvaluations
		if (!eval.equals("--")) {
			resultQtdEvaluations++
			if (eval.equals("MA")) resultMedia += 9
			else if (eval.equals("MPA")) resultMedia += 6
			else resultMedia += 3
		}
		[resultQtdEvaluations, resultMedia]
	}
	
	public boolean checkTotalAverage(double mediaAluno) {
		double media = 0
		for (Student student : Student.list()) {
			media += student.average
		}
		media = media / Student.list().size()
		return mediaAluno >= media
	}

	public boolean addEvaluationsToAllStudents(LinkedList<Evaluation> evaluationList) {
		for(Report reports : Report.list()){
			reports.students = []
		}
		for (int i = 0; i < Student.list().size(); i++) {
			Student.list().get(i).addEvaluation(evaluationList.get(i))
			Student.list().get(i).save(
					flush: true,
					failOnError: true
					)
			for (Report report : Report.list()) {
				if (report.tipo.equalsIgnoreCase("Porcentagem")) {
					checkConditionPercentage(Student.list().get(i).login, report)
				} else {
					checkConditionAverage(Student.list().get(i), report)
				}
			}
		}
		return true
	}

	public void addEvaluationsToStudentTests(String studentLogin, LinkedList<Evaluation> evaluationList){
		for (int i = 0; i < Student.list().size(); i++) {
			if(Student.list().get(i).login.equals(studentLogin)){
				Student.list().get(i).addEvaluation(evaluationList.get(0))
				Student.list().get(i).save(
						flush: true,
						failOnError: true
						)
			}
		}
	}

	public boolean evaluationTests(String studentLogin, String evaluationOrigin){
		def evaluation = Evaluation.findByOrigin(evaluationOrigin)
		List<Evaluation> listEval = new LinkedList<Evaluation>()
		listEval.add(evaluation)
		//addEvaluationsToAllStudents(studentLogin, listEval)
		addEvaluationsToStudentTests(studentLogin, listEval)
	}

	public boolean addEvaluationToAllStudents() {
		def evaluationInstance = new Evaluation(params);
		for (Student student : Student.findAll()) {
			student.addEvaluation(evaluationInstance);
			student.save flush: true
		}
		/*for(Student student : Student.list()){
		 student.addEvaluation(evaluationInstance.get(i));
		 student.save flush : true
		 }*/
		return true
	}

	public void checkConditionPercentage(String loginA, Report reportInstance) {
		double aux = checkPercentageEvaluationStudent(reportInstance.avaliacao, loginA)
		def controllerRepo = new ReportController()
		if (aux*100 >= reportInstance.valor) {
			Student student = Student.findByLogin(loginA)
			controllerRepo.addStudentToReport(student, reportInstance)
		}

	}

	public void checkConditionAverage(Student student, Report reportInstance) {
		def controllerRepo = new ReportController()
		if (checkTotalAverage(student.average)) {
			controllerRepo.addStudentToReport(student, reportInstance)
		}
	}

	public boolean updateEvaluation(String studentLogin, String newEvaluation, String criterionName, String evaluationOrigin){
		Student updatedStudent = Student.findByLogin(studentLogin)
		for(int i = 0; i < updatedStudent.criteriaAndEvaluations.size(); i++){
			if(updatedStudent.criteriaAndEvaluations.get(i).getCriterion().getDescription().equals(criterionName)){
				List<Evaluation> evaluationsInCriterion = updatedStudent.criteriaAndEvaluations.get(i).getEvaluations();
				for(int j = 0; j < evaluationsInCriterion.size(); j++){
					//if(evaluationsInCriterion.get(j) != null) {
					if (evaluationsInCriterion.get(j).getOrigin().equals(evaluationOrigin)) {
						evaluationsInCriterion.get(j).setValue(newEvaluation)
					}
					//}
				}
			}
		}
	}

	public int countAllStudents(){
		return Student.findAll().size();
	}

	public boolean saveStudent() {
		def studentInstance = new Student(params);
		if (Student.findByLogin(studentInstance.login) == null) {
			studentInstance.save flush: true
			return true
		}
		return false
	}

	@Transactional
	def createAndSaveStudent() {
		Student studentInstance = new Student(params)
		if (Student.findByLogin(studentInstance.getLogin()) == null) {
			if (studentInstance.hasErrors()) {
				respond studentInstance.errors, view: 'create'
				return
			}
			if(!studentInstance.save(flush: true)){
				render(view: "create", model: [studentInstance: studentInstance])
				return
			}
			flash.message = message(code: 'default.created.message', args: [
				message(code: 'student.label', default: 'Student'),
				studentInstance.id
			])
			redirect(action: "show", id: studentInstance.id)
		}
	}

	def addEvaluation2(String login, Evaluation evaluationInstance) {
		def student = Student.findByLogin(login)
		student.addEvaluation(evaluationInstance)
		student.save flush: true
	}

	public Student searchStudent() {
		def studentInstance = Student.findByLogin(params.login)
		return studentInstance
	}

	public Student searchStudent2(String login){
		def studentInstance = Student.findByLogin(login)
		return studentInstance
	}


	public Student createStudent() {
		return new Student(params)
	}

	def show(Student studentInstance) {
		respond studentInstance
	}

	public Student getStudent(String studentLogin){
		Student studentFound = Student.findByLogin(studentLogin)
		return studentFound
	}

	def create() {
		respond new Student(params)
	}


	def search() {
		render view: "search"
	}

	def consult() {
		def auxList = Student.list()
		def studentList = auxList.findAll {
			it.name.toLowerCase().contains(params.consult.toLowerCase()) || it.login.toLowerCase().contains(params.consult.toLowerCase())
		}
		if (studentList == null) {
			flash.message = message(code: 'default.not.found.message', args: [
				message(code: 'student.label', default: 'Student'),
				params.id
			])
			render view: "search", model: [studentInstanceList: [], studentInstanceCount: 0]
		} else {
			render view: "search", model: [studentInstanceList: studentList, studentInstanceCount: studentList.size()]
		}
	}

	@Transactional
	def savee(Student studentInstance) {
		if (studentInstance == null) {
			notFound()
			return
		}
		if (studentInstance.hasErrors()) {
			respond studentInstance.errors, view: 'create'
			return
		}
		studentInstance.save flush: true
		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.created.message', args: [
					message(code: 'student.label', default: 'Student'),
					studentInstance.id
				])
				redirect studentInstance
			}
			'*' { respond studentInstance, [status: CREATED] }
		}
	}

	def edit(Student studentInstance) {
		respond studentInstance
	}

	@Transactional
	def update(Student studentInstance) {
		if (studentInstance == null) {
			notFound()
			return
		}
		if (studentInstance.hasErrors()) {
			respond studentInstance.errors, view: 'edit'
			return
		}

		studentInstance.save flush: true

		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.updated.message', args: [
					message(code: 'student.label', default: 'Student'),
					studentInstance.id
				])
				redirect studentInstance
			}
			'*' { respond studentInstance, [status: OK] }
		}
	}

	@Transactional
	def delete(Student studentInstance) {

		if (studentInstance == null) {
			notFound()
			return
		}

		studentInstance.delete flush: true

		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.deleted.message', args: [
					message(code: 'student.label', default: 'Student'),
					studentInstance.id
				])
				redirect action: "index", method: "GET"
			}
			'*' { render status: NO_CONTENT }
		}
	}

	protected void notFound() {
		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.not.found.message', args: [
					message(code: 'student.label', default: 'Student'),
					params.id
				])
				redirect action: "index", method: "GET"
			}
			'*' { render status: NOT_FOUND }
		}
	}

	public void groupSave(List<Student> group) {
		for (int i = 0; i < group.size(); i++) {
			group.get(i).save flush: true;
		}
	}

	public String espacoBranco(String texto){
		for (int i = 0; i < texto.length(); i++){
			if(texto.charAt(i) == 160) {
				texto = texto.substring(i+1)
			}else{
				break
			}
		}
		for (int i = texto.length()-1; i > 0; i--){
			if(texto.charAt(i) == 32) {
				texto = texto.substring(0, texto.length()-1)
			}else{
				break
			}
		}
		return texto
	}

	def saveGroup() {
		String group = params.name
		String[] students = group.split(";")
		for (int i = 0; i < students.size(); i++) {
			List<String> token1 = students[i].tokenize(':')
			if(token1.size() <= 1) break
				String info = token1.get(0)
			List<String> token2 = info.tokenize('(')
			String name = token2.get(0)
			name = espacoBranco(name)
			String login = token2.get(1)
			login = login.replaceAll(" ","")
			Student novo = new Student(name, login)
			novo.calcMedia()

			if (Student.findByLogin(novo.getLogin()) == null) {
				novo.save flush: true
			}
		}

		redirect action: "index", method: "GET"
	}



	def createGroup() {
		respond view: 'createGroup'
	}
}




