INSERT INTO
  scheduler_task_config(name, schedulable_class, start_time, start_time_pattern,
                        repeat_interval, start_on_startup, started, created_by, date_created, uuid)
VALUES ('Bahmni Connect Artifact Publisher Task', 'org.bahmni.module.bahmniOfflineSync.job.InitialSyncArtifactsPublisher', DATE(NOW()) , 'MM/dd/yyyy HH:mm:ss', 86400, 1, 1, 1,  curdate(), uuid());