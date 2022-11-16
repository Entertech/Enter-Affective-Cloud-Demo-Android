package cn.entertech.flowtimezh.entity.meditation;

class ReportMeditationDataEntity {
    @Volatile var  reportPleasureEnitty: ReportPleasureEnitty? = null
    @Volatile var reportAttentionEnitty: ReportAttentionEnitty? = null
    @Volatile var reportPressureEnitty: ReportPressureEnitty? = null
    @Volatile var reportRelaxationEnitty: ReportRelaxationEnitty? = null
    @Volatile var reportHRDataEntity: ReportHRDataEntity? = null
    @Volatile var reportPEPRDataEntity: ReportPEPRDataEntity? = null
    @Volatile var reportEEGDataEntity: ReportEEGDataEntity? = null
    @Volatile var reportSleepEntity: ReportSleepEnitty? = null
    override fun toString(): String {
        return "ReportMeditationDataEntity(reportPleasureEnitty=$reportPleasureEnitty, reportAttentionEnitty=$reportAttentionEnitty, reportPressureEnitty=$reportPressureEnitty, reportRelaxationEnitty=$reportRelaxationEnitty, reportHRDataEntity=$reportHRDataEntity, reportPEPRDataEntity=$reportPEPRDataEntity, reportEEGDataEntity=$reportEEGDataEntity, reportSleepEntity=$reportSleepEntity)"
    }


}