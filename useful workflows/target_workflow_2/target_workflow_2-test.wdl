version 1.0

workflow assemble_refbased {

    meta {
        description: "A test workflow, simulating inputs and outputs of `assemble_refbased`, to help build out CBAS functionality"
    }

    parameter_meta {}

    input {
        Array[File]+ file_array
        File         file_array[0]
        String       first_file_name = basename(file_array[0], '.txt')

        String       optional_string = "foo"
        File?        optional_file_1
        Int          optional_int = 3
        Float        optional_float = 0.75
        Boolean      optional_bool = false
        File?        optional_file_2
    }


    output {
        File        result_file_1                                = file_array[0]
        File        result_file_2                                = file_array[0]
        Int         result_int_1                                 = optional_int
        Int         result_int_2                                 = optional_int
        Int         result_int_3                                 = optional_int
        Float       result_float_1                               = optional_float
        Int         result_int_4                             = call_consensus.dist_to_ref_snps
        Int         result_int_5                           = call_consensus.dist_to_ref_indels
        
        Array[Int]                result_int_array      = [optional_int]
        Array[Float]              result_float_array    = [optional_float]
        Array[Map[String,String]] ivar_trim_stats                = ivar_stats
        Array[Array[String]]      ivar_trim_stats_tsv            = ivar_stats_row
        
        Int         replicate_concordant_sites                   = run_discordance.concordant_sites
        Int         replicate_discordant_snps                    = run_discordance.discordant_snps
        Int         replicate_discordant_indels                  = run_discordance.discordant_indels
        Int         num_read_groups                              = run_discordance.num_read_groups
        Int         num_libraries                                = run_discordance.num_libraries
        File        replicate_discordant_vcf                     = run_discordance.discordant_sites_vcf
        
        Array[File] align_to_ref_per_input_aligned_flagstat      = align_to_ref.aligned_bam_flagstat
        Array[Int]  align_to_ref_per_input_reads_provided        = align_to_ref.reads_provided
        Array[Int]  align_to_ref_per_input_reads_aligned         = align_to_ref.reads_aligned
        Array[File] align_to_ref_per_input_fastqc                = align_to_ref.aligned_only_reads_fastqc
        
        File        align_to_ref_merged_aligned_trimmed_only_bam = aligned_trimmed_bam
        File        align_to_ref_merged_coverage_plot            = plot_ref_coverage.coverage_plot
        File        align_to_ref_merged_coverage_tsv             = plot_ref_coverage.coverage_tsv
        Int         align_to_ref_merged_reads_aligned            = plot_ref_coverage.reads_aligned
        Int         align_to_ref_merged_read_pairs_aligned       = plot_ref_coverage.read_pairs_aligned
        Float       align_to_ref_merged_bases_aligned            = plot_ref_coverage.bases_aligned
        File        align_to_ref_isnvs_vcf                       = isnvs_ref.report_vcf
        
        File        picard_metrics_wgs                           = alignment_metrics.wgs_metrics
        File        picard_metrics_alignment                     = alignment_metrics.alignment_metrics
        File        picard_metrics_insert_size                   = alignment_metrics.insert_size_metrics
        File        samtools_ampliconstats                       = alignment_metrics.amplicon_stats
        File        samtools_ampliconstats_parsed                = alignment_metrics.amplicon_stats_parsed

        Array[File] align_to_self_merged_aligned_and_unaligned_bam = align_to_self.aligned_bam

        File        align_to_self_merged_aligned_only_bam        = aligned_self_bam
        File        align_to_self_merged_coverage_plot           = plot_self_coverage.coverage_plot
        File        align_to_self_merged_coverage_tsv            = plot_self_coverage.coverage_tsv
        Int         align_to_self_merged_reads_aligned           = plot_self_coverage.reads_aligned
        Int         align_to_self_merged_read_pairs_aligned      = plot_self_coverage.read_pairs_aligned
        Float       align_to_self_merged_bases_aligned           = plot_self_coverage.bases_aligned
        Float       align_to_self_merged_mean_coverage           = plot_self_coverage.mean_coverage
        File        align_to_self_isnvs_vcf                      = isnvs_self.report_vcf
        
        String      align_to_ref_viral_core_version              = align_to_ref.viralngs_version[0]
        String      ivar_version                                 = ivar_trim.ivar_version[0]
        String      viral_assemble_version                       = call_consensus.viralngs_version
    }

}
