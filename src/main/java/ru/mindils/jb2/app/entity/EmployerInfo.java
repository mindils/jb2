package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@JmixEntity
@Table(name = "JB2_EMPLOYER_INFO")
@Entity(name = "jb2_EmployerInfo")
@Getter
@Setter
public class EmployerInfo {

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @InstanceName
    @Column(name = "STATUS")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    @PrimaryKeyJoinColumn
    public Employer employer;

    public EmployerStatus getStatus() {
        return status == null ? null : EmployerStatus.fromId(status);
    }

    public void setStatus(EmployerStatus status) {
        this.status = status == null ? null : status.getId();
    }

}