@extends('admin.layout.master')
@section('styles')
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/dropzone/5.5.1/min/dropzone.min.css">
    @endsection
@section('content')

    <section class="content-header">
        @include('admin.layout.form-errors')
        <h1>
            ایجاد دسته بندی جدید
        </h1>
        <div class="col-md-10 offset-md-1" style="margin: 110px;">
            <form method="POST" action="{{route('photos.store')}}" enctype="multipart/form-data" class="dropzone" id="dropzone">
                @csrf
            </form></div>

@endsection
@section('scripts')
            <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/dropzone/5.5.1/min/dropzone.min.js">
                Dropzone.options.dropzone =
                    {
                        maxFilesize: 12,
                        renameFile: function(file) {
                            var dt = new Date();
                            var time = dt.getTime();
                            return time+file.name;
                        },
                        acceptedFiles: ".jpeg,.jpg,.png,.gif",
                        addRemoveLinks: true,
                        timeout: 5000,
                        removedfile: function(file)
                        {
                            var name = file.upload.filename;
                            $.ajax({
                                headers: {
                                    'X-CSRF-TOKEN': $('meta[name="_token"]').attr('content')
                                },
                                type: 'POST',
                                url: '{{ route('photos.destroy') }}',
                                data: {filename: name},
                                success: function (data){
                                    console.log("File has been successfully removed!!");
                                },
                                error: function(e) {
                                    console.log(e);
                                }});
                            var fileRef;
                            return (fileRef = file.previewElement) != null ?
                                fileRef.parentNode.removeChild(file.previewElement) : void 0;
                        },
                        success: function(file, response)
                        {
                            console.log(response);
                        },
                        error: function(file, response)
                        {
                            return false;
                        }
                    };
                </script>
@endsection
