package ta

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

class ReportController {

	static allowedMethods = [update: "PUT"]

	def index(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		respond Report.list(params), model: [ReportInstanceCount : Report.count()]
	}

	def show(Report reportInstance){
		respond reportInstance
	}

	def create(){
		respond new Report(params)
	}

	def search() {
		render view: "search"
	}
	
	public Report searchReport(String nome){
		def reportInstance = Report.findByName(nome)
		return reportInstance
	}
	
	def consult() {
		def auxList = Report.list()
		def reportList = auxList.findAll {
			it.name.toLowerCase().contains(params.consult.toLowerCase()) || it.name.toLowerCase().contains(params.consult.toLowerCase())
		}
		if (reportList == null) {
			flash.message = message(code: 'default.not.found.message', args: [
				message(code: 'report.label', default: 'Report'),
				params.id
			])
			render view: "search", model: [reportInstanceList: [], reportInstanceCount: 0]
		} else if (!reportList) {
			flash.message = message(code: 'default.not.found.message', args: [
				message(code: 'report.label', default: 'Report'),
				params.id
			])
			render view: "search", model: [reportInstanceList: [], reportInstanceCount: 0]
		}else {
			render view: "search", model: [reportInstanceList: reportList, reportInstanceCount: reportList.size()]
		}
	}
	
	public void addStudentToReport(Student student, Report reportInstance){
		if(reportInstance!=null){
			reportInstance.addToStudents(student)
			reportInstance.save(flush: true)
			flash.message = message(code: 'default.updated.message', args:[
				message(code: 'Report.label', default: 'Report'),
				reportInstance.id
			])
		}
	}

	def delete(Report reportInstance) {

		if (reportInstance == null) {
			notFound()
			return
		}

		reportInstance.delete flush:true

		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.deleted.message', args: [
					message(code: 'Report.label', default: 'Report'),
					reportInstance.id
				])
				redirect action:"index", method:"GET"
			}
			'*'{ render status: NO_CONTENT }
		}
	}

	def save(){
		def reportInstance = new Report(params)
		if(!reportInstance.save(flush:true)){
			render (view: "show", model: [reportInstance: reportInstance])
			return
		}
		if(reportInstance.hasErrors()){
			respond reportInstance.errors, view:'show'
		}
		reportInstance.fillReport()
		reportInstance.save(flush: true)
		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.updated.message', args: [
					message(code: 'Report.label', default: 'Report'),
					reportInstance.id
				])
				redirect reportInstance
			}
			'*'{ respond reportInstance, [status: OK] }
		}
	}

	def update(Report reportInstance) {
		if (reportInstance == null) {
			notFound()
			return
		}

		if (reportInstance.hasErrors()) {
			respond reportInstance.errors, view:'show'
			return
		}

		reportInstance.save flush:true

		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.updated.message', args: [
					message(code: 'Report.label', default: 'Report'),
					reportInstance.id
				])
				redirect reportInstance
			}
			'*'{ respond reportInstance, [status: OK] }
		}
	}

	def edit(Report reportInstance) {
		respond reportInstance
	}

	protected void notFound() {
		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.not.found.message', args: [
					message(code: 'report.label', default: 'Report'),
					params.id
				])
				redirect action: "show", method: "GET"
			}
			'*'{ render status: NOT_FOUND }
		}
	}
}
