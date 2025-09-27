package ru.mindils.jb2.app.mapper;

import org.mapstruct.*;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueue;

import java.util.List;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GenericTaskQueueMapper {

  /**
   * Конвертирует entity в DTO для безопасной передачи в Temporal
   */
  GenericTaskQueueDto toDto(GenericTaskQueue entity);

  /**
   * Конвертирует DTO в entity (если понадобится)
   */
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "lastModifiedDate", ignore = true)
  GenericTaskQueue toEntity(GenericTaskQueueDto dto);

  /**
   * Конвертирует список entities в список DTOs
   */
  List<GenericTaskQueueDto> toDtoList(List<GenericTaskQueue> entities);

  /**
   * Конвертирует список DTOs в список entities
   */
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "lastModifiedDate", ignore = true)
  List<GenericTaskQueue> toEntityList(List<GenericTaskQueueDto> dtos);
}