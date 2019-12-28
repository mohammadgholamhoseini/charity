@extends('admin.layout.master')
@section('styles')
    <link href="{{asset('css/dropzone.min.css')}}" rel="stylesheet">
@endsection
@section('content')

    <section class="content-header" style="padding-bottom: 60px">
        @include('admin.layout.form-errors')
        <h1>
            ایجاد دسته بندی جدید
        </h1>
        <div class="row">
            <div class="col-md-9">
                <form method="POST" action="{{route('charities.store')}}" style="margin-right: 275px">
                    @csrf
                    <div class="form-group">
                        <label>عنوان</label>
                        <input type="text" class="form-control" name="name">
                    </div>
                    <div class="form-group">
                        <label>نام سرپرست</label>
                        <input type="text" class="form-control" name="owner">
                    </div>
                    <div class="form-group">
                        <label>تعداد اعضا</label>
                        <input type="number" class="form-control" name="members">
                    </div>
                    <div class="form-group">
                        <label>شماره حساب</label>
                        <input type="text" class="form-control" name="account_number">
                    </div>
                    <div class="form-group">
                        <label>توضیحات</label>
                        <textarea class="form-control" name="description"></textarea>
                    </div>
                    <div class="form-group">
                        <label>ایمیل</label>
                        <input type="email" class="form-control" name="email">
                    </div>
                    <div class="form-group">
                        <label>آدرس</label>
                        <input type="text" class="form-control" name="address">
                    </div>
                    <div class="form-group">
                        <label>شماره تلفن</label>
                        <input type="text" class="form-control" name="phone">
                    </div>
                    <div class="form-group">
                        <label>نام مستعار</label>
                        <input type="text" class="form-control" name="slug">
                    </div>
                    <div class="form-group">
                        <label>متا-توضیحات</label>
                        <textarea class="form-control" name="meta_desc"></textarea>
                    </div>

                    <div class="form-group">
                        <label>متا-کلمات</label>
                        <textarea class="form-control" name="meta_key"></textarea>
                    </div>
                    <div class="form-group">
                        <label>متا-عنوان</label>
                        <textarea class="form-control" name="meta_title"></textarea>
                    </div>
                    <div class="form-group">
                    <label>دسته بندی ها</label>
                        <select multiple name="category">
                            @foreach($categories as $category)
                            <option class="form-controll"  value="{{$category->id}}">{{$category->name}}</option>
                            @endforeach

                        </select>
                    </div>
                    <div class="form-group">
                        <label>تصویر محصول</label>
                        <input type="hidden" name="photo_id[]" id="product_photo">
                        <div class="dropzone" id="photo"></div>
                    </div>
                    <div class="form-group">
                    <input type="submit" class="btn btn-success" value="ثبت" style="float:left">
                    </div>
                </form>

            </div>
        </div>

    </section>
@endsection
@section('scripts')
    <script src="{{asset('js/dropzone.min.js')}}" type="application/javascript"></script>

    <script>
        Dropzone.autoDiscover= false;
        var photos = [];
        var myDropzone = new Dropzone("#photo", {
            addRemoveLinks: true,
            url: "{{route('photos.store')}}",
            sending: function (file, xhr, formData) {
                formData.append("_token", "{{csrf_token()}}")
            },
            success: function (file, response) {
                photos.push(response.photo_id)

            }
        });
        </script>
@endsection

