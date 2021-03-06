package controllers;

import entities.jpa.dao.CourseDAO;
import entities.jpa.dao.StudentDAO;
import entities.jpa.models.Course;
import entities.jpa.models.Student;
import lombok.Getter;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.List;

@Named
@ConversationScoped
public class ConversationUseCaseControllerCdi implements Serializable {

    private static final String PAGE_INDEX_REDIRECT = "conversation-cdi?faces-redirect=true";

    private enum CURRENT_FORM {
        CREATE_COURSE, CREATE_STUDENT, CONFIRMATION
    }

    @Inject
    private EntityManager em;

    @Inject
    @Getter
    private Conversation conversation;

    @Inject
    private CourseDAO courseDAO;
    @Inject
    private StudentDAO studentDAO;

    @Getter
    private Course course = new Course();
    @Getter
    private Student student = new Student();

    private CURRENT_FORM currentForm = CURRENT_FORM.CREATE_COURSE;
    public boolean isCurrentForm(CURRENT_FORM form) {
        return currentForm == form;
    }

    /**
     * The first conversation step.
     */
    public void createCourse() {
        conversation.begin();
        currentForm = CURRENT_FORM.CREATE_STUDENT;
    }

    /**
     * The second conversation step.
     */
    public void createStudent() {
        student.getCourseList().add(course);
        course.getStudentList().add(student);
        currentForm = CURRENT_FORM.CONFIRMATION;
    }

    /**
     * The last conversation step.
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public String ok() {
        try {
            courseDAO.create(course);
            studentDAO.create(student);
            em.flush();
            Messages.addGlobalInfo("Success!");
        } catch (OptimisticLockException ole) {
            // Other user was faster...
            Messages.addGlobalWarn("Please try again");
        } catch (PersistenceException pe) {
            // Some problems with DB - most often this is programmer's fault.
            Messages.addGlobalError("Finita la commedia...");
        }
        Faces.getFlash().setKeepMessages(true);
        conversation.end();
        return PAGE_INDEX_REDIRECT;
    }

    /**
     * The last (alternative) conversation step.
     */
    public String cancel() {
        conversation.end();
        return PAGE_INDEX_REDIRECT;
    }

    public List<Student> getAllStudents() {
        return studentDAO.getAllStudents();
    }
}
